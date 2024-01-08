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

public class InvoiceStack extends Stack implements BaseDockerBuild {
	

	
	private static final  String S3_BUCKET_KEY = "S3_BUCKET_KEY";
	
	private static final  String WEB_SOCKET_API_GATEWAY_KEY = "WEB_SOCKET_API_GATEWAY_KEY";
	

	public InvoiceStack(final Construct scope, final String id, final EcommerceCommons ecommerceCommons, StackProps stackProps) {
		super(scope, id, stackProps);
		
		 final Map<String, String> environments = new HashMap<>();
		 environments.put(DynamoDbStack.INVOICE_DDB_KEY, DynamoDbStack.TABLE_INVOICE);
		 environments.put(DynamoDbStack.EVENTS_DDB_KEY, DynamoDbStack.TABLE_EVENT);
		 environments.put(EventBridgeStack.AUDIT_EVENT_BRIDGE_KEY, EventBridgeStack.AUDIT_EVENT_BRIDGE_VALUE);
		 environments.put(EventBridgeStack.FAIL_CHECK_INVOICE_KEY, EventBridgeStack.FAIL_CHECK_INVOICE_NUMBER_VALUE);
		 environments.put(EventBridgeStack.INVOICE_TIMEOUT_KEY, EventBridgeStack.INVOICE_TIMEOUT_VALUE);		 
		 environments.put(EventBridgeStack.INVOICE_SOURCE_EVENT_BRIDGE_KEY, EventBridgeStack.INVOICE_SOURCE_EVENT_VALUE);
		 
		 this.createInitWebSocketFunctions(ecommerceCommons);
		 
		 final Bucket bucket = this.createS3Bucket(environments);
			
		 final WebSocketApi invoiceWebSocketApi = this.createWebSocket(ecommerceCommons, environments);
		 
		 // Recupera a layer ecommerce para ser vinculada as funções
	     final String ecommerceLayerArn = StringParameter.valueForStringParameter(this, LambdaLayersStack.ECOMMERCE_LAYER_VERSION_ARN);
		 final ILayerVersion ecommerceLayer = LayerVersion.fromLayerVersionArn(this, LambdaLayersStack.ECOMMERCE_LAYER_VERSION_ARN, ecommerceLayerArn);
			
		 // Crias outros funções da stack acessadas pelo websocket
		 ecommerceCommons.setInvoiceGetUrlFunction(new Function(this, "InvoiceGetUrlFunction", FunctionProps.builder()
		                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
		                .functionName("InvoiceGetUrlFunction")
		                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
		                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
		                        .build()))
		                .handler("com.br.aws.ecommerce.invoice.InvoiceGetUrlFunction")
		                .memorySize(256)
		                .environment(environments)
		                .insightsVersion(LambdaInsightsVersion.VERSION_1_0_119_0)
		                .layers(Arrays.asList(ecommerceLayer))
		                .tracing(Tracing.ACTIVE)
		                .timeout(Duration.seconds(20))
		                .logRetention(RetentionDays.ONE_WEEK)
		                .build()));
			  

			  
		  ecommerceCommons.getInvoiceGetUrlFunction().addToRolePolicy(PolicyStatement.Builder.create()
						.effect(Effect.ALLOW)
						.actions(Arrays.asList("s3:PutObject"))
						.resources(Arrays.asList(bucket.getBucketArn()+ "/*"))
						.build());
			  
			invoiceWebSocketApi.grantManageConnections(ecommerceCommons.getInvoiceGetUrlFunction());
			  
			invoiceWebSocketApi.grantManageConnections(ecommerceCommons.getInvoiceDefaultFunction());
			  
			  
			 ecommerceCommons.setInvoiceImportFunction(new Function(this, "InvoiceImportFunction", FunctionProps.builder()
		                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
		                .functionName("InvoiceImportFunction")
		                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
		                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
		                        .build()))
		                .handler("com.br.aws.ecommerce.invoice.InvoiceImportFunction")
		                .memorySize(256)
		                .environment(environments)
		                .insightsVersion(LambdaInsightsVersion.VERSION_1_0_119_0)
		                .layers(Arrays.asList(ecommerceLayer))
		                .tracing(Tracing.ACTIVE)
		                .timeout(Duration.seconds(20))
		                .logRetention(RetentionDays.ONE_WEEK)
		                .build()));
			  

			  
			  bucket.addEventNotification(EventType.OBJECT_CREATED_PUT, new LambdaDestination(ecommerceCommons.getInvoiceImportFunction()));
			  
			  
			  ecommerceCommons.getInvoiceImportFunction().addToRolePolicy(PolicyStatement.Builder.create()
						.effect(Effect.ALLOW)
						.actions(Arrays.asList("s3:DeleteObject", "s3:GetObject"))
						.resources(Arrays.asList(bucket.getBucketArn()+ "/*"))
						.build());
			  
			  invoiceWebSocketApi.grantManageConnections(ecommerceCommons.getInvoiceImportFunction());
			  
			  
			  ecommerceCommons.setInvoiceCancelImportFunction(new Function(this, "InvoiceCancelImportFunction", FunctionProps.builder()
		                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
		                .functionName("InvoiceCancelImportFunction")
		                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
		                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
		                        .build()))
		                .handler("com.br.aws.ecommerce.invoice.InvoiceCancelImportFunction")
		                .memorySize(256)
		                .environment(environments)
		                .insightsVersion(LambdaInsightsVersion.VERSION_1_0_119_0)
		                .layers(Arrays.asList(ecommerceLayer))
		                .tracing(Tracing.ACTIVE)
		                .timeout(Duration.seconds(20))
		                .logRetention(RetentionDays.ONE_WEEK)
		                .build()));
			  
			  invoiceWebSocketApi.grantManageConnections(ecommerceCommons.getInvoiceCancelImportFunction());
			 		  

			  
			  // Web Socket API Routes
		      invoiceWebSocketApi.addRoute("getImportUrl", WebSocketRouteOptions.builder()
			          .integration(new WebSocketLambdaIntegration("InvoiceGetUrlFunctionIntegration", ecommerceCommons.getInvoiceGetUrlFunction()))
			          .build());
		      
		      invoiceWebSocketApi.addRoute("cancelImport", WebSocketRouteOptions.builder()
			          .integration(new WebSocketLambdaIntegration("InvoiceCancelImportFunctionIntegration", ecommerceCommons.getInvoiceCancelImportFunction()))
			          .build());
		      
		      
			  ecommerceCommons.setInvoiceEventFunction(new Function(this, "InvoiceEventFunction", FunctionProps.builder()
		                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
		                .functionName("InvoiceEventFunction")
		                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
		                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
		                        .build()))
		                .handler("com.br.aws.ecommerce.invoice.InvoiceEventFunction")
		                .memorySize(256)
		                .environment(environments)
		                .insightsVersion(LambdaInsightsVersion.VERSION_1_0_119_0)
		                .layers(Arrays.asList(ecommerceLayer))
		                .tracing(Tracing.ACTIVE)
		                .timeout(Duration.seconds(20))
		                .logRetention(RetentionDays.ONE_WEEK)
		                .build()));
			  
			  invoiceWebSocketApi.grantManageConnections(ecommerceCommons.getInvoiceEventFunction());
		
		  
	}
	
	
	private void createInitWebSocketFunctions(final EcommerceCommons ecommerceCommons) {
		

		  ecommerceCommons.setInvoiceConnectionFunction(new Function(this, "InvoiceConnectionFunction", FunctionProps.builder()
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
	                .build()));
		  
		  
		  ecommerceCommons.setInvoiceDisconnectionFunction(new Function(this, "InvoiceDisconnectionFunction", FunctionProps.builder()
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
	                .build()));
		  
		  
		  ecommerceCommons.setInvoiceDefaultFunction(new Function(this, "InvoiceDefaultFunction", FunctionProps.builder()
	                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
	                .functionName("InvoiceDefaultFunction")
	                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
	                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
	                        .build()))
	                .handler("com.br.aws.ecommerce.invoice.InvoiceDefaultFunction")
	                .memorySize(256)
	                .tracing(Tracing.ACTIVE)
	                .timeout(Duration.seconds(20))
	                .logRetention(RetentionDays.ONE_WEEK)
	                .build()));
		
	}
	
	
	private WebSocketApi createWebSocket(final EcommerceCommons ecommerceCommons, final Map<String, String> environments) {
		
		final WebSocketApi invoiceWebSocketApi  = new WebSocketApi(this, "ecommerce-websocket-apigateway", 
				  WebSocketApiProps.builder()
				  .apiName("ecommerce-websocket-apigateway")
				  
				  .defaultRouteOptions(WebSocketRouteOptions
						  .builder()
						  .integration(new WebSocketLambdaIntegration("DefaultHandler", ecommerceCommons.getInvoiceDefaultFunction()))
						  .build())
				  
				  .connectRouteOptions(WebSocketRouteOptions
						  .builder()
						  .integration(new WebSocketLambdaIntegration("ConnectionHandler", ecommerceCommons.getInvoiceConnectionFunction()))
						  .build())
				  .disconnectRouteOptions(WebSocketRouteOptions
						  .builder()
						  .integration(new WebSocketLambdaIntegration("DisconnectionHandler", ecommerceCommons.getInvoiceDisconnectionFunction()))
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
			  
	    return invoiceWebSocketApi;
	}
	
	 
	
	private Bucket createS3Bucket(final Map<String, String> environments) {
		
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
		   
		   return bucket;
		   
		    
	}

}
