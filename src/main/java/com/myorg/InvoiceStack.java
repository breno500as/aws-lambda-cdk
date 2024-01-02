package com.myorg;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.aws_apigatewayv2_integrations.WebSocketLambdaIntegration;
import software.amazon.awscdk.services.apigatewayv2.WebSocketApi;
import software.amazon.awscdk.services.apigatewayv2.WebSocketApiProps;
import software.amazon.awscdk.services.apigatewayv2.WebSocketRouteOptions;
import software.amazon.awscdk.services.apigatewayv2.WebSocketStage;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.ILayerVersion;
import software.amazon.awscdk.services.lambda.LambdaInsightsVersion;
import software.amazon.awscdk.services.lambda.LayerVersion;
import software.amazon.awscdk.services.lambda.Tracing;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.EventType;
import software.amazon.awscdk.services.s3.LifecycleRule;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.amazon.awscdk.services.s3.notifications.LambdaDestination;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

public class InvoiceStack extends Stack implements DockerBuildStack {
	
	private static final String TABLE_INVOICE = "invoice";
	
	private static final String INVOICE_DDB_KEY = "INVOICE_DDB_KEY";
	
	private static final  String S3_BUCKET_KEY = "S3_BUCKET_KEY";
	
	private static final  String WEB_SOCKET_API_GATEWAY_KEY = "WEB_SOCKET_API_GATEWAY_KEY";
	

	public InvoiceStack(final Construct scope, final String id, StackProps stackProps) {
		super(scope, id, stackProps);
		
		 final Map<String, String> environments = new HashMap<>();
		 environments.put(INVOICE_DDB_KEY, TABLE_INVOICE);
		
 
		final Table invoiceTable = Table.Builder.create(this, "InvoiceTable")
				.tableName(TABLE_INVOICE)
				.readCapacity(1)
				.writeCapacity(1)
				.billingMode(BillingMode.PROVISIONED)
				.partitionKey(Attribute.builder().name("pk").type(AttributeType.STRING).build())
				.sortKey(Attribute.builder().name("sk").type(AttributeType.STRING).build())
				.timeToLiveAttribute("ttl")
				.removalPolicy(RemovalPolicy.DESTROY).build();
		
		
		final Bucket bucket = Bucket.Builder
				.create(this, "InvoiceBucket")
				.removalPolicy(RemovalPolicy.DESTROY)
				.autoDeleteObjects(true)
				.lifecycleRules(Arrays.asList(LifecycleRule.builder()
				        .id("deleteAfterOneDay")
				        .expiration(Duration.days(1))
				        .enabled(true)
				        .build()))
				.build();
		
		   environments.put(S3_BUCKET_KEY, bucket.getBucketName());
		   
		   // Recupera a layer ecommerce para ser vinculada as funções
		   final String ecommerceLayerArn = StringParameter.valueForStringParameter(this, LambdaLayersStack.ECOMMERCE_LAYER_VERSION_ARN);
		   final ILayerVersion ecommerceLayer = LayerVersion.fromLayerVersionArn(this, LambdaLayersStack.ECOMMERCE_LAYER_VERSION_ARN, ecommerceLayerArn);
		
		  final Function invoiceConnectionFunction =  new Function(this, "InvoiceConnectionFunction", FunctionProps.builder()
	                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
	                .functionName("InvoiceConnectionFunction")
	                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
	                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
	                        .build()))
	                .handler("com.br.aws.ecommerce.invoice.InvoiceConnectionFunction")
	                .memorySize(256)
	                .tracing(Tracing.ACTIVE)
	                .timeout(Duration.seconds(10))
	                .logRetention(RetentionDays.ONE_WEEK)
	                .build());
		  
		  
		  final Function invoiceDisconnectionFunction = new Function(this, "InvoiceDisconnectionFunction", FunctionProps.builder()
	                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
	                .functionName("InvoiceDisconnectionFunction")
	                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
	                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
	                        .build()))
	                .handler("com.br.aws.ecommerce.invoice.InvoiceDisconnectionFunction")
	                .memorySize(256)
	                .tracing(Tracing.ACTIVE)
	                .timeout(Duration.seconds(10))
	                .logRetention(RetentionDays.ONE_WEEK)
	                .build());
		  
		  final WebSocketApi invoiceWebSocketApi = new WebSocketApi(this, "ecommerce-websocket-apigateway", 
				  WebSocketApiProps.builder()
				  .apiName("ecommerce-websocket-apigateway")
				  .connectRouteOptions(WebSocketRouteOptions
						  .builder()
						  .integration(new WebSocketLambdaIntegration("ConnectionHandler", invoiceConnectionFunction))
						  .build())
				  .disconnectRouteOptions(WebSocketRouteOptions
						  .builder()
						  .integration(new WebSocketLambdaIntegration("DisconnectionHandler", invoiceDisconnectionFunction))
						  .build())
				  .build());
		  
		  final String stage = "prod";
		  
		  final String webSocketApi = invoiceWebSocketApi.getApiEndpoint() + "/" + stage;
		  
		  WebSocketStage.Builder.create(this, "InvoiceWebSocketStage")
		          .webSocketApi(invoiceWebSocketApi)
		          .stageName(stage)
		          .autoDeploy(true)
		          .build();
		  
		  environments.put(WEB_SOCKET_API_GATEWAY_KEY, webSocketApi);
		  
		  final Function invoiceGetUrlFunction = new Function(this, "InvoiceGetUrlFunction", FunctionProps.builder()
	                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
	                .functionName("InvoiceGetUrlFunction")
	                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
	                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
	                        .build()))
	                .handler("com.br.aws.ecommerce.invoice.InvoiceGetUrlFunction")
	                .memorySize(512)
	                .environment(environments)
	                .insightsVersion(LambdaInsightsVersion.VERSION_1_0_119_0)
	                .layers(Arrays.asList(ecommerceLayer))
	                .tracing(Tracing.ACTIVE)
	                .timeout(Duration.seconds(20))
	                .logRetention(RetentionDays.ONE_WEEK)
	                .build());
		  
		  invoiceTable.grantWriteData(invoiceGetUrlFunction);
		  
		  invoiceGetUrlFunction.addToRolePolicy(PolicyStatement.Builder.create()
					.effect(Effect.ALLOW)
					.actions(Arrays.asList("s3:PutObject"))
					.resources(Arrays.asList(bucket.getBucketArn()+ "/*"))
					.build());
		  
		  invoiceWebSocketApi.grantManageConnections(invoiceGetUrlFunction);
		  
		  
		  final Function invoiceImportFunction = new Function(this, "InvoiceImportFunction", FunctionProps.builder()
	                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
	                .functionName("InvoiceImportFunction")
	                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
	                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
	                        .build()))
	                .handler("com.br.aws.ecommerce.invoice.InvoiceImportFunction")
	                .memorySize(512)
	                .environment(environments)
	                .insightsVersion(LambdaInsightsVersion.VERSION_1_0_119_0)
	                .layers(Arrays.asList(ecommerceLayer))
	                .tracing(Tracing.ACTIVE)
	                .timeout(Duration.seconds(20))
	                .logRetention(RetentionDays.ONE_WEEK)
	                .build());
		  
		  invoiceTable.grantReadWriteData(invoiceImportFunction);
		  
		  bucket.addEventNotification(EventType.OBJECT_CREATED_PUT, new LambdaDestination(invoiceImportFunction));
		  
		  
		  invoiceImportFunction.addToRolePolicy(PolicyStatement.Builder.create()
					.effect(Effect.ALLOW)
					.actions(Arrays.asList("s3:DeleteObject", "s3:GetObject"))
					.resources(Arrays.asList(bucket.getBucketArn()+ "/*"))
					.build());
		  
		  invoiceWebSocketApi.grantManageConnections(invoiceImportFunction);
		  
		  
		  final Function invoiceCancelImportFunction = new Function(this, "InvoiceCancelImportFunction", FunctionProps.builder()
	                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
	                .functionName("InvoiceCancelImportFunction")
	                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
	                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
	                        .build()))
	                .handler("com.br.aws.ecommerce.invoice.InvoiceCancelImportFunction")
	                .memorySize(512)
	                .environment(environments)
	                .insightsVersion(LambdaInsightsVersion.VERSION_1_0_119_0)
	                .layers(Arrays.asList(ecommerceLayer))
	                .tracing(Tracing.ACTIVE)
	                .timeout(Duration.seconds(20))
	                .logRetention(RetentionDays.ONE_WEEK)
	                .build());
		  
		  invoiceWebSocketApi.grantManageConnections(invoiceCancelImportFunction);
		  
		  invoiceTable.grantReadWriteData(invoiceCancelImportFunction);
		  
		  // Web Socket API Routes
	      invoiceWebSocketApi.addRoute("getImportUrl", WebSocketRouteOptions.builder()
		          .integration(new WebSocketLambdaIntegration("InvoiceGetUrlFunctionIntegration", invoiceGetUrlFunction))
		          .build());
	      
	      invoiceWebSocketApi.addRoute("cancelImport", WebSocketRouteOptions.builder()
		          .integration(new WebSocketLambdaIntegration("InvoiceCancelImportFunctionIntegration", invoiceCancelImportFunction))
		          .build());


	}

}
