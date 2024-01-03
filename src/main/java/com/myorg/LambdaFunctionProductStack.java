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




public class LambdaFunctionProductStack extends Stack implements DockerBuildStack {
	


 
	public LambdaFunctionProductStack(final Construct scope, final String id, EcommerceCommons ecommerceCommons, final StackProps props) {
		
		   super(scope, id, props);
 
		   
		   final Map<String, String> environments = new HashMap<>();
		   environments.put(LambdaFunctionEventsStack.EVENTS_FUNCTION_KEY, LambdaFunctionEventsStack.EVENTS_FUNCTION_VALUE);
		   environments.put(DynamoDbStack.PRODUCTS_DDB, DynamoDbStack.TABLE_PRODUCT);
		   environments.put(AwsLambdaCdkApp.POWERTOOLS_SERVICE_KEY, AwsLambdaCdkApp.POWERTOOLS_SERVICE_VALUE);
		   
		   
		   final String ecommerceLayerArn = StringParameter.valueForStringParameter(this, LambdaLayersStack.ECOMMERCE_LAYER_VERSION_ARN);    
		   final ILayerVersion ecommerceLayer = LayerVersion.fromLayerVersionArn(this, LambdaLayersStack.ECOMMERCE_LAYER_VERSION_ARN, ecommerceLayerArn);
		 
		 
		    // Função 1 - ProductsFetchFunction
		    ecommerceCommons.setProductsFetchFunction(new Function(this, "ProductFetchFunction", FunctionProps.builder()
	                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
	                .functionName("ProductsFetchFunction")
	                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
	                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
	                        .build()))
	                .handler("com.br.aws.ecommerce.product.ProductFetchFunction")
	                .memorySize(256)
	                .insightsVersion(LambdaInsightsVersion.VERSION_1_0_119_0)
	                .timeout(Duration.seconds(20))
	                .tracing(Tracing.ACTIVE)
	                .environment(environments)
	                .layers(Arrays.asList(ecommerceLayer))
	                .logRetention(RetentionDays.ONE_WEEK)
	                .build()));
		        
	        
	        // Função 2 - ProductsAdminFunction    
		    ecommerceCommons.setProductsAdminFunction(new Function(this, "ProductAdminFunction", FunctionProps.builder()
	                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
	                .functionName("ProductsAdminFunction")
	                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
	                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
	                        .build()))
	                .handler("com.br.aws.ecommerce.product.ProductAdminFunction")
	                .memorySize(256)
	                .environment(environments)
	                .insightsVersion(LambdaInsightsVersion.VERSION_1_0_119_0)
	                .timeout(Duration.seconds(40))
	                .logRetention(RetentionDays.ONE_WEEK)
	                .tracing(Tracing.ACTIVE)
	                .layers(Arrays.asList(ecommerceLayer))
	                .build()));
		
	}
	
	 

	 

}
