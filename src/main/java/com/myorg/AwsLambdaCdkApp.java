package com.myorg;

import java.util.HashMap;
import java.util.Map;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class AwsLambdaCdkApp {

	private static final String REGION = "us-east-1";

	private static final String ACCOUNT = "596058222797";

	public static void main(final String[] args) {
		App app = new App();

		final Map<String, String> tags = new HashMap<String, String>();
		tags.put("Ecommerce", "Ecommerce team");

		final StackProps stackProps = StackProps.builder()
				                                .tags(tags)
				                                .env(Environment.builder()
				                                		         .account(ACCOUNT)
				                                		         .region(REGION)
				                                		         .build())
				                                .build();

		final ProductCommonsStack productCommonsStack = new ProductCommonsStack();

		final LambdaFunctionProductStack lambdaFunctionProductStack = new LambdaFunctionProductStack(app,
				"LambdaFunctionProductStack", productCommonsStack, stackProps);

		final ApiGatewayProductStack apiGatewayProductStack = new ApiGatewayProductStack(app, "ApiGatewayProductStack",productCommonsStack, stackProps);
		apiGatewayProductStack.addDependency(lambdaFunctionProductStack);

		app.synth();
	}
}
