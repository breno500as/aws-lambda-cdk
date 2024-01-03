package com.myorg;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.ILayerVersion;
import software.amazon.awscdk.services.lambda.LambdaInsightsVersion;
import software.amazon.awscdk.services.lambda.LayerVersion;
import software.amazon.awscdk.services.lambda.Tracing;
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.amazon.awscdk.services.sns.StringConditions;
import software.amazon.awscdk.services.sns.SubscriptionFilter;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.subscriptions.LambdaSubscription;
import software.amazon.awscdk.services.sns.subscriptions.SqsSubscription;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.Queue;
import software.amazon.awscdk.services.sqs.QueueEncryption;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

public class LambdaFunctionOrdersStack extends Stack implements DockerBuildStack {
	
	
	public static final String ORDERS_FUNCTION_KEY = "ORDERS_FUNCTION_KEY";
	
	public static final String ORDERS_FUNCTION_VALUE = "OrdersFunction";
	
	public static final String ORDERS_EVENT_FUNCTION_KEY = "ORDERS_EVENT_FUNCTION_KEY";
	
	public static final String ORDERS_EVENT_FUNCTION_VALUE = "OrdersEventFunction";
	
	public static final String ORDERS_PAYMENT_FUNCTION_KEY = "ORDERS_PAYMENT_FUNCTION_KEY";
	
	public static final String ORDERS_PAYMENT_FUNCTION_VALUE = "OrdersPaymentFunction";
	
	public static final String ORDERS_EMAIL_FUNCTION_KEY = "ORDERS_EMAIL_FUNCTION_KEY";
	
	public static final String ORDERS_EMAIL_FUNCTION_VALUE = "OrdersEmailFunction";
	
	public static final String ORDERS_EVENT_FETCH_FUNCTION_KEY = "ORDERS_EVENT_FETCH_FUNCTION_KEY";
	
	public static final String ORDERS_EVENT_FETCH_FUNCTION_VALUE = "OrdersEventFetchFunction";
 
	public LambdaFunctionOrdersStack(final Construct scope, final String id, EcommerceCommons ecommerceCommons, final StackProps props) {
		
		       super(scope, id, props);
		       
		       
		       // Cria um tópico SNS
		       final SnsTopic orderEventSnsTopic = SnsTopic.Builder
						.create(Topic.Builder.create(this, "OrderEventsTopic").topicName("order-events-topic").build()).build();

		       
				this.createFunctions(ecommerceCommons, orderEventSnsTopic);
			  
			   orderEventSnsTopic.getTopic().grantPublish(ecommerceCommons.getOrdersFunction());
			   
			   // Subscriber 1 - Adiciona a função order events como subscriber da função de orders
			   orderEventSnsTopic.getTopic().addSubscription(LambdaSubscription.Builder
					   .create(ecommerceCommons.getOrdersEventFunction())
					   .build());
			   
			   // Adiciona um filtro de notificações SQS
			   final Map<String, SubscriptionFilter> filterPoliciesSqsSns = new HashMap<>();
			   filterPoliciesSqsSns.put("eventType", SubscriptionFilter.stringFilter(StringConditions
					   .builder()
					   .allowlist(Arrays.asList("ORDER_CREATED"))
					   .build()));
			   
			   // Subscriber 2 -Adiciona a função order payments como subscriber da função de orders
			   orderEventSnsTopic.getTopic().addSubscription(LambdaSubscription.Builder
					   .create(ecommerceCommons.getOrdersPaymentFunction())
					   .filterPolicy(filterPoliciesSqsSns)
					   .build());
			   
			   // Cria uma DLQ para fila SQS
			   final Queue orderDlqQueue = Queue.Builder.create(this, "OrderEventsDlqQueue")
			  		   .queueName("order-events-dlq-queue")
			  		   .enforceSsl(false)
			  		  .retentionPeriod(Duration.days(10))
			  	       .encryption(QueueEncryption.UNENCRYPTED)
			  		   .build();
			     
			     final DeadLetterQueue orderDeadLetterQueue = DeadLetterQueue.builder()
			              .queue(orderDlqQueue)
			              .maxReceiveCount(3)
			              .build();
			   
			   // Cria uma fila SQS		   
			   final Queue orderEventQueue = Queue.Builder.create(this, "OrderEventsQueue")
					   .queueName("order-events-queue")
					   .encryption(QueueEncryption.UNENCRYPTED)
					   .deadLetterQueue(orderDeadLetterQueue)
					   .deadLetterQueue(null)
					   .enforceSsl(false)
					   .build();
			   
			     
			   
			   
			   // Subscriber 3 -Adiciona a fila SQS como subscriber da função de orders
			   orderEventSnsTopic.getTopic().addSubscription(SqsSubscription.Builder.create(orderEventQueue).filterPolicy(filterPoliciesSqsSns).build());
			   
			   // Adiciona a função de email como fonte de eventos da fila SQS
			   ecommerceCommons.getOrdersEmailFunction().addEventSource(SqsEventSource.Builder.create(orderEventQueue)
					                      .batchSize(5) // Deixa acumular 5 notificações SNS antes de invocar a função lambda enviando a mensagem
					                      .enabled(true) 
					                      .maxBatchingWindow(Duration.minutes(1)) // Timeout para envio de mensagens para função lambda, ou seja envia mensagens a cada 5 notificações ou a cada 1 minuto
					                      .build());
			   
			   orderEventQueue.grantConsumeMessages(ecommerceCommons.getOrdersEmailFunction());
			   
				// Adiciona a permissão de envio de emails para função de OrderEmailFunction
				ecommerceCommons.getOrdersEmailFunction().addToRolePolicy(PolicyStatement.Builder.create()
						.effect(Effect.ALLOW)
						.actions(Arrays.asList("ses:SendEmail", "ses:SendRawEmail"))
						.resources(Arrays.asList("*"))
						.build());
			   
	}
	
	private void createFunctions(EcommerceCommons ecommerceCommons, final SnsTopic orderEventSnsTopic) {
		
		   // Passa variáveis de ambiente para as funções
		   final Map<String, String> environments = new HashMap<>();
		   environments.put(DynamoDbStack.ORDERS_DDB, DynamoDbStack.TABLE_ORDER);
		   environments.put(DynamoDbStack.PRODUCTS_DDB, DynamoDbStack.TABLE_PRODUCT);
		   environments.put(AwsLambdaCdkApp.POWERTOOLS_SERVICE_KEY, AwsLambdaCdkApp.POWERTOOLS_SERVICE_VALUE);
		   environments.put("ORDER_EVENTS_TOPIC_ARN", orderEventSnsTopic.getTopic().getTopicArn());
		   
		   // Recupera a layer ecommerce para ser vinculada as funções
		   final String ecommerceLayerArn = StringParameter.valueForStringParameter(this, LambdaLayersStack.ECOMMERCE_LAYER_VERSION_ARN);
		   final ILayerVersion ecommerceLayer = LayerVersion.fromLayerVersionArn(this, LambdaLayersStack.ECOMMERCE_LAYER_VERSION_ARN, ecommerceLayerArn);
		   
		   ecommerceCommons.setOrdersFunction(new Function(this, ORDERS_FUNCTION_VALUE, FunctionProps.builder()
                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
                .functionName(ORDERS_FUNCTION_VALUE)
                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
                        .build()))
                .handler("com.br.aws.ecommerce.order.OrderFunction")
                .memorySize(256)
                .tracing(Tracing.ACTIVE)
                .insightsVersion(LambdaInsightsVersion.VERSION_1_0_119_0)
                .timeout(Duration.seconds(20))
                .environment(environments)
                .layers(Arrays.asList(ecommerceLayer))
                .logRetention(RetentionDays.ONE_WEEK)
                .build()));  
		   
		
		   ecommerceCommons.setOrdersEventFunction(new Function(this, ORDERS_EVENT_FUNCTION_VALUE, FunctionProps.builder()
	                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
	                .functionName(ORDERS_EVENT_FUNCTION_VALUE)
	                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
	                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
	                        .build()))
	                .handler("com.br.aws.ecommerce.order.OrderEventFunction")
	                .memorySize(256)
	                .tracing(Tracing.ACTIVE)
	                .insightsVersion(LambdaInsightsVersion.VERSION_1_0_119_0)
	                .timeout(Duration.seconds(20))
	                .environment(environments)
	                .layers(Arrays.asList(ecommerceLayer))
	                .logRetention(RetentionDays.ONE_WEEK)
	                .build()));
		   
		   ecommerceCommons.setOrdersEventFetchFunction(new Function(this, ORDERS_EVENT_FETCH_FUNCTION_VALUE, FunctionProps.builder()
	                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
	                .functionName(ORDERS_EVENT_FETCH_FUNCTION_VALUE)
	                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
	                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
	                        .build()))
	                .handler("com.br.aws.ecommerce.event.OrderEventFetchFunction")
	                .memorySize(256)
	                .tracing(Tracing.ACTIVE)
	                .insightsVersion(LambdaInsightsVersion.VERSION_1_0_119_0)
	                .timeout(Duration.seconds(20))
	                .environment(environments)
	                .layers(Arrays.asList(ecommerceLayer))
	                .logRetention(RetentionDays.ONE_WEEK)
	                .build()));
		   
		   ecommerceCommons.setOrdersPaymentFunction(new Function(this, ORDERS_PAYMENT_FUNCTION_VALUE, FunctionProps.builder()
	                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
	                .functionName(ORDERS_PAYMENT_FUNCTION_VALUE)
	                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
	                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
	                        .build()))
	                .handler("com.br.aws.ecommerce.order.OrderPaymentFunction")
	                .memorySize(256)
	                .tracing(Tracing.ACTIVE)
	                .insightsVersion(LambdaInsightsVersion.VERSION_1_0_119_0)
	                .timeout(Duration.seconds(10))
	                .environment(environments)
	                .layers(Arrays.asList(ecommerceLayer))
	                .logRetention(RetentionDays.ONE_WEEK)
	                .build()));
		   	   
	
		   ecommerceCommons.setOrdersEmailFunction(new Function(this, ORDERS_EMAIL_FUNCTION_VALUE, FunctionProps.builder()
	                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
	                .functionName(ORDERS_EMAIL_FUNCTION_VALUE)
	                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
	                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
	                        .build()))
	                .handler("com.br.aws.ecommerce.order.OrderEmailFunction")
	                .memorySize(256)
	                .tracing(Tracing.ACTIVE)
	                .insightsVersion(LambdaInsightsVersion.VERSION_1_0_119_0)
	                .timeout(Duration.seconds(20))
	                .environment(environments)
	                .layers(Arrays.asList(ecommerceLayer))
	                .logRetention(RetentionDays.ONE_WEEK)
	                .build()));
		
	}
	
	 

}
