
package com.myorg;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.BaseArchiveProps;
import software.amazon.awscdk.services.events.EventBus;
import software.amazon.awscdk.services.events.EventBusProps;
import software.amazon.awscdk.services.events.EventPattern;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.RuleProps;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.events.targets.SqsQueue;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.LambdaInsightsVersion;
import software.amazon.awscdk.services.lambda.Tracing;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.amazon.awscdk.services.sqs.Queue;
import software.amazon.awscdk.services.sqs.QueueEncryption;
import software.constructs.Construct;

public class EventBridgeStack extends Stack implements DockerBuildStack {

	private EventBus eventBus;
	
	public static final String ORDER_SOURCE_EVENT_BRIDGE_KEY = "ORDER_SOURCE_EVENT_BRIDGE_KEY";
	
	public static final String ORDER_SOURCE_EVENT_VALUE = "app.order";
	
	public static final String INVOICE_SOURCE_EVENT_BRIDGE_KEY = "INVOICE_SOURCE_EVENT_BRIDGE_KEY";
	
	public static final String INVOICE_SOURCE_EVENT_VALUE = "app.invoice";
	
	public static final String PRODUCT_NOT_FOUND_KEY = "PRODUCT_NOT_FOUND_KEY";
	
	public static final String PRODUCT_NOT_FOUND_VALUE = "PRODUCT_NOT_FOUND";
	
	public static final String FAIL_CHECK_INVOICE_KEY = "FAIL_CHECK_INVOICE_KEY";
	
	public static final String FAIL_CHECK_INVOICE_NUMBER_VALUE = "FAIL_CHECK_INVOICE_NUMBER";
	
	public static final String INVOICE_TIMEOUT_KEY = "INVOICE_TIMEOUT_KEY";
	
	public static final String INVOICE_TIMEOUT_VALUE = "INVOICE_TIMEOUT";
	
	public static final String AUDIT_EVENT_BRIDGE_KEY = "AUDIT_EVENT_BRIDGE_KEY";
	
	public static final String AUDIT_EVENT_BRIDGE_VALUE = "AuditEventBridge";

	public EventBridgeStack(final Construct scope, final String id, EcommerceCommons ecommerceCommons,
			StackProps stackProps) {

		super(scope, id, stackProps);
		
		final Map<String, String> environments = new HashMap<>();
		 
		// Event Bus
		this.eventBus = new EventBus(this, AUDIT_EVENT_BRIDGE_VALUE,
				EventBusProps.builder().eventBusName(AUDIT_EVENT_BRIDGE_VALUE).build());

		// Event Bus Archive - Forma de armazenamento e reenvio de evento
		this.eventBus.archive("BusArquive", BaseArchiveProps.builder()
				.eventPattern(EventPattern.builder()
						      .source(Arrays.asList(ORDER_SOURCE_EVENT_VALUE))
						      .build())
				.archiveName("AuditEvents")
				.retention(Duration.days(10))
				.build());
		
		this.createRuleTargetOrder(environments);
		this.createRuleTargetInvoice(environments, ecommerceCommons);
		
		this.eventBus.grantPutEventsTo(ecommerceCommons.getOrdersFunction());
		this.eventBus.grantPutEventsTo(ecommerceCommons.getInvoiceImportFunction());
		this.eventBus.grantPutEventsTo(ecommerceCommons.getInvoiceEventFunction());
 
	}
	
	private void createRuleTargetOrder(final Map<String, String> environments) {
		
		// Rule Order
		final Map<String, Object> detailRule = new HashMap<>();
		detailRule.put("reason", Arrays.asList(PRODUCT_NOT_FOUND_VALUE));
				
			
		final Rule nonValidOrderRule = new Rule(this, "NonValidOrderRule", RuleProps.builder()
						          .ruleName("NonValidOrderRule")
						          .description("Rule matching none valid order")
						          .eventBus(this.eventBus).eventPattern(EventPattern.builder()
							      .source(Arrays.asList(ORDER_SOURCE_EVENT_VALUE))
							      .detailType(Arrays.asList("order"))
							      .detail(detailRule)
							      .build()).
						 build());
				
		 // Target Order
		 final Function orderErrorsFunction = new Function(this, "OrdersErrorsFunction", FunctionProps.builder()
		                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
		                .functionName("OrdersErrorsFunction")
		                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
		                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
		                        .build()))
		                .handler("com.br.aws.ecommerce.audit.errors.OrdersErrorsFunction")
		                .memorySize(256)
		                .environment(environments)
		                .insightsVersion(LambdaInsightsVersion.VERSION_1_0_119_0)
		                .timeout(Duration.seconds(10))
		                .logRetention(RetentionDays.ONE_WEEK)
		                .tracing(Tracing.ACTIVE)
		                .build());
				
		  nonValidOrderRule.addTarget(new LambdaFunction(orderErrorsFunction));
		
	}
	
    private void createRuleTargetInvoice(final Map<String, String> environments, EcommerceCommons ecommerceCommons) {
    	
    	final String invoiceDetail = "invoice";
		
		// Rule details Invoice invalid number
		final Map<String, Object> detailNonValidInvoiceRule = new HashMap<>();
		detailNonValidInvoiceRule.put("reason", Arrays.asList(FAIL_CHECK_INVOICE_NUMBER_VALUE));

		
	
		// Rule non valid invoice	
		final Rule nonValidInvoiceRule = new Rule(this, "NonValidInvoiceRule", RuleProps.builder()
						          .ruleName("NonValidInvoiceRule")
						          .description("Rule matching none valid invoice")
						          .eventBus(this.eventBus).eventPattern(EventPattern.builder()
							      .source(Arrays.asList(INVOICE_SOURCE_EVENT_VALUE))
							      .detailType(Arrays.asList(invoiceDetail))
							      .detail(detailNonValidInvoiceRule)
							      .build()).
						 build());
				
	     // Target non valid invoice	
		 final Function invoiceErrorsFunction = new Function(this, "InvoicesErrorsFunction", FunctionProps.builder()
		                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
		                .functionName("InvoicesErrorsFunction")
		                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
		                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
		                        .build()))
		                .handler("com.br.aws.ecommerce.audit.errors.InvoicesErrorsFunction")
		                .memorySize(256)
		                .environment(environments)
		                .insightsVersion(LambdaInsightsVersion.VERSION_1_0_119_0)
		                .timeout(Duration.seconds(10))
		                .logRetention(RetentionDays.ONE_WEEK)
		                .tracing(Tracing.ACTIVE)
		                .build());
				
		 nonValidInvoiceRule.addTarget(new LambdaFunction(invoiceErrorsFunction));
		 
		 
		// Rule details Invoice timeout
		final Map<String, Object> detailInvoiceTimeoutRule = new HashMap<>();
		detailInvoiceTimeoutRule.put("reason", Arrays.asList(INVOICE_TIMEOUT_VALUE));
		 
	    // Rule timeout invoice
		final Rule timeoutInvoiceRule = new Rule(this, "TimeoutInvoiceRule", RuleProps.builder()
			          .ruleName("TimeoutInvoiceRule")
			          .description("Rule matching timeout invoice")
			          .eventBus(this.eventBus).eventPattern(EventPattern.builder()
				      .source(Arrays.asList(INVOICE_SOURCE_EVENT_VALUE))
				      .detailType(Arrays.asList(invoiceDetail))
				      .detail(detailInvoiceTimeoutRule)
				      .build()).
			 build());
		
		 // Target timeout invoice	
		ecommerceCommons.setInvoiceTimeoutQueue(Queue.Builder.create(this, "InvoiceTimeoutQueue")
		  		   .queueName("invoice-timeout-queue")
		  		   .enforceSsl(false)
		           .retentionPeriod(Duration.days(10))
		  	       .encryption(QueueEncryption.UNENCRYPTED)
		  		   .build());
		  
		 
		timeoutInvoiceRule.addTarget(new SqsQueue(ecommerceCommons.getInvoiceTimeoutQueue()));
		
	}
    
    
 

}
