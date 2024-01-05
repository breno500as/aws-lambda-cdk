package com.myorg;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.ILayerVersion;
import software.amazon.awscdk.services.lambda.LambdaInsightsVersion;
import software.amazon.awscdk.services.lambda.LayerVersion;
import software.amazon.awscdk.services.lambda.Tracing;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;




public class ProductStack extends Stack implements DockerBuildStack {
	

	public static final String PRODUCT_EVENT_FUNCTION_KEY = "PRODUCT_EVENT_FUNCTION_KEY";
	
	public static final String PRODUCT_EVENT_FUNCTION_VALUE = "ProductEventFunction";
	
	public static final String PRODUCT_FETCH_FUNCTION_VALUE = "ProductFetchFunction";
	
	public static final String PRODUCT_ADMIN_FUNCTION_VALUE = "ProductAdminFunction";
	

 
	public ProductStack(final Construct scope, final String id, EcommerceCommons ecommerceCommons, final StackProps props) {
		
		   super(scope, id, props);
 
		   
		   final Map<String, String> environments = new HashMap<>();
		   environments.put(DynamoDbStack.EVENTS_DDB_KEY, DynamoDbStack.TABLE_EVENT);
		   environments.put(PRODUCT_EVENT_FUNCTION_KEY, PRODUCT_EVENT_FUNCTION_VALUE);
		   environments.put(DynamoDbStack.PRODUCTS_DDB_KEY, DynamoDbStack.TABLE_PRODUCT);
		   environments.put(AwsLambdaCdkApp.POWERTOOLS_SERVICE_KEY, AwsLambdaCdkApp.POWERTOOLS_SERVICE_VALUE);
		   
		   
		   final String ecommerceLayerArn = StringParameter.valueForStringParameter(this, LambdaLayersStack.ECOMMERCE_LAYER_VERSION_ARN);    
		   final ILayerVersion ecommerceLayer = LayerVersion.fromLayerVersionArn(this, LambdaLayersStack.ECOMMERCE_LAYER_VERSION_ARN, ecommerceLayerArn);
		 
		 
		    // Função 1 - ProductsFetchFunction
		    ecommerceCommons.setProductsFetchFunction(new Function(this, PRODUCT_FETCH_FUNCTION_VALUE, FunctionProps.builder()
	                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
	                .functionName(PRODUCT_FETCH_FUNCTION_VALUE)
	                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
	                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
	                        .build()))
	                .handler("com.br.aws.ecommerce.product.ProductFetchFunction")
	                .memorySize(256)
	                .insightsVersion(LambdaInsightsVersion.VERSION_1_0_119_0)
	                .timeout(Duration.seconds(10))
	                .tracing(Tracing.ACTIVE)
	                .environment(environments)
	                .layers(Arrays.asList(ecommerceLayer))
	                .logRetention(RetentionDays.ONE_WEEK)
	                .build()));
		        
	        
	        // Função 2 - ProductsAdminFunction    
		    ecommerceCommons.setProductsAdminFunction(new Function(this, PRODUCT_ADMIN_FUNCTION_VALUE, FunctionProps.builder()
	                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
	                .functionName(PRODUCT_ADMIN_FUNCTION_VALUE)
	                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
	                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
	                        .build()))
	                .handler("com.br.aws.ecommerce.product.ProductAdminFunction")
	                .memorySize(256)
	                .environment(environments)
	                .insightsVersion(LambdaInsightsVersion.VERSION_1_0_119_0)
	                .timeout(Duration.seconds(30))
	                .logRetention(RetentionDays.ONE_WEEK)
	                .tracing(Tracing.ACTIVE)
	                .layers(Arrays.asList(ecommerceLayer))
	                .build()));
		    
		    
		      // Função 3 - EventsFunction - Eventos de produtos  
			  ecommerceCommons.setProductEventFunction(new Function(this, PRODUCT_EVENT_FUNCTION_VALUE, FunctionProps.builder()
		                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
		                .functionName(PRODUCT_EVENT_FUNCTION_VALUE)
		                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
		                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
		                        .build()))
		                .handler("com.br.aws.ecommerce.product.ProductEventFunction")
		                .memorySize(256)
		                .tracing(Tracing.ACTIVE)
		                .insightsVersion(LambdaInsightsVersion.VERSION_1_0_119_0)
		                .timeout(Duration.seconds(10))
		                .environment(environments)
		                .layers(Arrays.asList(ecommerceLayer))
		                .logRetention(RetentionDays.ONE_WEEK)
		                .build()));  
			   
			   ecommerceCommons.getProductEventFunction().grantInvoke(ecommerceCommons.getProductsAdminFunction());
		
	}
	
	 

	 

}
