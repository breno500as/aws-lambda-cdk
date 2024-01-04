package com.myorg;

import java.util.HashMap;
import java.util.Map;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.lambda.Runtime;

public class AwsLambdaCdkApp {

	private static final String REGION = "us-east-1";

	private static final String ACCOUNT = "596058222797";
	
	public static final String PROJECT_LAMBDA_FUNCTIONS_NAME = "aws-ecommerce-lambda-functions";
	
	public static final String POWERTOOLS_SERVICE_KEY = "POWERTOOLS_SERVICE_NAME";
	
	public static final String POWERTOOLS_SERVICE_VALUE = "Ecommerce x-ray tracing";
	
	public static final Runtime PROJECT_JAVA_RUNTIME = Runtime.JAVA_17;

	public static void main(final String[] args) {
		App app = new App();

		final Map<String, String> tags = new HashMap<String, String>();
		tags.put("Team", "Developers");
		tags.put("Cost", "Ecommerce");

		final StackProps stackProps = StackProps.builder()
				                                .tags(tags)
				                                .env(Environment.builder()
				                                		         .account(ACCOUNT)
				                                		         .region(REGION)
				                                		         .build())
				                                .build();

		final EcommerceCommons ecommerceCommons = new EcommerceCommons();
		
		final LambdaLayersStack ecommerceLayersStack = new LambdaLayersStack(app, "LambdaLayersStack", stackProps);

		final ProductStack productStack = new ProductStack(app, "ProductStack", ecommerceCommons, stackProps);
		productStack.addDependency(ecommerceLayersStack);
		
		final OrderStack orderStack = new OrderStack(app, "OrderStack", ecommerceCommons, stackProps);
		orderStack.addDependency(ecommerceLayersStack);
		
		final ApiGatewayStack apiGatewayStack = new ApiGatewayStack(app, "ApiGatewayStack",ecommerceCommons, stackProps);
	 	apiGatewayStack.addDependency(productStack);
	 	apiGatewayStack.addDependency(orderStack);
		
	 	new DynamoDbStack(app, "DynamoDbStack", ecommerceCommons, stackProps);
	 	
	 	new InvoiceStack(app, "InvoiceStack", stackProps);
		 

		app.synth();
	}
}
