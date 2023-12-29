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
import software.amazon.awscdk.services.lambda.LambdaInsightsVersion;
import software.amazon.awscdk.services.lambda.LayerVersion;
import software.amazon.awscdk.services.lambda.Tracing;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

public class LambdaFunctionEventsStack extends Stack implements DockerBuildStack {
	
	
	public static final String PRODUCT_EVENTS_FUNCTION_NAME = "PRODUCT_EVENTS_FUNCTION_NAME";
	
	public static final String EVENTS_FUNCTION_NAME = "EventsFunction";
	

	public LambdaFunctionEventsStack(final Construct scope, final String id, EcommerceCommons ecommerceCommons, final StackProps props) {
		
		       super(scope, id, props);
		   
			   final Map<String, String> environments = new HashMap<>();
			 
			   environments.put(DynamoDbStack.EVENTS_DDB, DynamoDbStack.TABLE_EVENT);
			   environments.put(AwsLambdaCdkApp.POWERTOOLS_SERVICE_NAME, AwsLambdaCdkApp.POWERTOOLS_SERVICE_VALUE);
			   
			   final String eventLayerArn = StringParameter.valueForStringParameter(this, LambdaLayersStack.EVENTS_LAYER_VERSION_ARN);
			    
		   
			   ecommerceCommons.setEventsFunction(new Function(this, "EventsFunction", FunctionProps.builder()
	                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
	                .functionName(EVENTS_FUNCTION_NAME)
	                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
	                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
	                        .build()))
	                .handler("com.br.aws.ecommerce.event.ProductEventFunction")
	                .memorySize(512)
	                .tracing(Tracing.ACTIVE)
	                .insightsVersion(LambdaInsightsVersion.VERSION_1_0_119_0)
	                .timeout(Duration.seconds(20))
	                .environment(environments)
	                .layers(Arrays.asList(LayerVersion.fromLayerVersionArn(this, LambdaLayersStack.EVENTS_LAYER_VERSION_ARN, eventLayerArn)))
	                .logRetention(RetentionDays.ONE_WEEK)
	                .build()));  
			   
			   ecommerceCommons.getEventsFunction().grantInvoke(ecommerceCommons.getProductsAdminFunction());
			   
		   
	}

}
