package com.myorg;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.LambdaInsightsVersion;
import software.amazon.awscdk.services.lambda.LayerVersion;
import software.amazon.awscdk.services.lambda.Tracing;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

public class LambdaFunctionOrdersStack extends Stack implements DockerBuildStack {
	
	
	public static final String ORDERS_FUNCTION_KEY = "ORDERS_FUNCTION_NAME";
	
	public static final String ORDERS_FUNCTION_VALUE = "OrdersFunction";
	

	public LambdaFunctionOrdersStack(final Construct scope, final String id, EcommerceCommons ecommerceCommons, final StackProps props) {
		
		       super(scope, id, props);
		       
		       final SnsTopic orderSnsTopic = SnsTopic.Builder
						.create(Topic.Builder.create(this, "OrderEventsTopic").topicName("order-events").build()).build();

				
		   
			   final Map<String, String> environments = new HashMap<>();
			 
			   environments.put(DynamoDbStack.ORDERS_DDB, DynamoDbStack.TABLE_ORDER);
			   environments.put(DynamoDbStack.PRODUCTS_DDB, DynamoDbStack.TABLE_PRODUCT);
			   environments.put(AwsLambdaCdkApp.POWERTOOLS_SERVICE_KEY, AwsLambdaCdkApp.POWERTOOLS_SERVICE_VALUE);
			   environments.put("ORDER_EVENTS_TOPIC_ARN", orderSnsTopic.getTopic().getTopicArn());
			   
			   final String ecommerceLayerArn = StringParameter.valueForStringParameter(this, LambdaLayersStack.ECOMMERCE_LAYER_VERSION_ARN);
			    
		   
			   ecommerceCommons.setOrdersFunction(new Function(this, ORDERS_FUNCTION_VALUE, FunctionProps.builder()
	                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
	                .functionName(ORDERS_FUNCTION_VALUE)
	                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
	                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
	                        .build()))
	                .handler("com.br.aws.ecommerce.order.OrdersFunction")
	                .memorySize(512)
	                .tracing(Tracing.ACTIVE)
	                .insightsVersion(LambdaInsightsVersion.VERSION_1_0_119_0)
	                .timeout(Duration.seconds(20))
	                .environment(environments)
	                .layers(Arrays.asList(LayerVersion.fromLayerVersionArn(this, LambdaLayersStack.ECOMMERCE_LAYER_VERSION_ARN, ecommerceLayerArn)))
	                .logRetention(RetentionDays.ONE_WEEK)
	                .build()));  
			   
			   orderSnsTopic.getTopic().grantPublish(ecommerceCommons.getOrdersFunction());
			     
		   
	}

}
