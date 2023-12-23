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
import software.amazon.awscdk.services.lambda.LayerVersion;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;




public class LambdaFunctionProductStack extends Stack implements DockerBuildStack {
	

	private static final String PRODUCTS_DDB = "PRODUCTS_DDB";
	
 
	
	
	public LambdaFunctionProductStack(final Construct scope, final String id, ProductCommons productCommonsStack, final StackProps props) {
		
		   super(scope, id, props);
 
		  
		   
		   final Map<String, String> environments = new HashMap<>();
		   environments.put(PRODUCTS_DDB, DynamoDbStack.TABLE_PRODUCT);
		   
		   final String productLayerArn = StringParameter.valueForStringParameter(this, LambdaLayersProductStack.PROJECT_LAYER_VERSION_ARN);
		    
		    ILayerVersion productLayer = LayerVersion.fromLayerVersionArn(this, LambdaLayersProductStack.PROJECT_LAYER_VERSION_ARN, productLayerArn);
		 
		 
		    
		    // Função 1 - ProductsFetchFunction
		    productCommonsStack.setProductsFetchFunction(new Function(this, "ProductsFetchFunction", FunctionProps.builder()
	                .runtime(Runtime.JAVA_11)
	                .functionName("ProductsFetchFunction")
	                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
	                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
	                        .build()))
	                .handler("com.br.aws.ecommerce.product.ProductsFetchFunction")
	                .memorySize(512)
	                .timeout(Duration.seconds(10))
	                .environment(environments)
	                .layers(Arrays.asList(productLayer))
	                .logRetention(RetentionDays.ONE_WEEK)
	                .build()));
		    
		    
		  
		       
	        
	        // Função 2 - ProductsAdminFunction    
		    productCommonsStack.setProductsAdminFunction(new Function(this, "ProductsAdminFunction", FunctionProps.builder()
	                .runtime(Runtime.JAVA_11)
	                .functionName("ProductsAdminFunction")
	                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
	                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
	                        .build()))
	                .handler("com.br.aws.ecommerce.product.ProductsAdminFunction")
	                .memorySize(512)
	                .environment(environments)
	                .timeout(Duration.seconds(10))
	                .logRetention(RetentionDays.ONE_WEEK)
	                .layers(Arrays.asList(productLayer))
	                .build()));
		
	}
	
	 

	 

}
