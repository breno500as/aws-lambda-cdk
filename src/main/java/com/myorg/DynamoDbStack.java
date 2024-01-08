package com.myorg;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.EnableScalingProps;
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps;
import software.amazon.awscdk.services.dynamodb.ProjectionType;
import software.amazon.awscdk.services.dynamodb.StreamViewType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.UtilizationScalingProps;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.StartingPosition;
import software.amazon.awscdk.services.lambda.eventsources.DynamoEventSource;
import software.amazon.awscdk.services.lambda.eventsources.SqsDlq;
import software.amazon.awscdk.services.sqs.Queue;
import software.amazon.awscdk.services.sqs.QueueEncryption;
import software.constructs.Construct;

public class DynamoDbStack  extends Stack {
	
	public static final String PRODUCTS_DDB_KEY = "PRODUCTS_DDB_KEY";
	
	public static final String ORDERS_DDB_KEY = "ORDERS_DDB_KEY";

	public static final String EVENTS_DDB_KEY = "EVENTS_DDB_KEY";

	public static final String TABLE_PRODUCT = "product";

	public static final String TABLE_EVENT = "event";

	public static final String TABLE_ORDER = "order";
	
	public static final String TABLE_INVOICE = "invoice";
	
	public static final String INVOICE_DDB_KEY = "INVOICE_DDB_KEY";
	
	
	public DynamoDbStack(final Construct scope, final String id, EcommerceCommons ecommerceCommons, StackProps stackProps) {
		super(scope, id, stackProps);
	
		this.createProductTable(ecommerceCommons);
	    this.createEventsTable(ecommerceCommons);
	    this.createOrderTable(ecommerceCommons);
	    this.createInvoiceTable(ecommerceCommons);
		
	}
	
	
	private void createProductTable(EcommerceCommons ecommerceCommons) {
		
		final Table productTable = Table.Builder.create(this, "ProductTable")
				.tableName(TABLE_PRODUCT)
				.readCapacity(1)
				.writeCapacity(1)
				.billingMode(BillingMode.PROVISIONED)
				.partitionKey(Attribute.builder().name("id").type(AttributeType.STRING).build())
				.removalPolicy(RemovalPolicy.DESTROY).build();

		productTable.autoScaleReadCapacity(EnableScalingProps.builder()
				                                      .minCapacity(1)
				                                      .maxCapacity(4)
				                                      .build())
				          .scaleOnUtilization(UtilizationScalingProps.builder().targetUtilizationPercent(50)
						                                                       .scaleInCooldown(Duration.seconds(30))
						                                                       .scaleOutCooldown(Duration.seconds(30))
						                                                       .build());
		
		
		productTable.autoScaleWriteCapacity(EnableScalingProps.builder()
				                                                   .minCapacity(1)
				                                                   .maxCapacity(4)
				                                                   .build())
		                  .scaleOnUtilization(UtilizationScalingProps.builder().targetUtilizationPercent(50)
				                                                               .scaleInCooldown(Duration.seconds(30))
				                                                               .scaleOutCooldown(Duration.seconds(30))
				                                                               .build());
		
		productTable.grantReadData(ecommerceCommons.getProductsFetchFunction());
		productTable.grantWriteData(ecommerceCommons.getProductsAdminFunction());
		productTable.grantReadData(ecommerceCommons.getOrdersFunction());
		

	}
	
	private void createEventsTable(EcommerceCommons ecommerceCommons) {
	
		final Table eventsTable = Table.Builder.create(this, "EventTable")
				.tableName(TABLE_EVENT)
				.readCapacity(1)
				.writeCapacity(1)
				.billingMode(BillingMode.PROVISIONED)
				.partitionKey(Attribute.builder().name("pk").type(AttributeType.STRING).build())
				.sortKey(Attribute.builder().name("sk").type(AttributeType.STRING).build())
				.timeToLiveAttribute("ttl")
				.removalPolicy(RemovalPolicy.DESTROY).build();

		eventsTable.autoScaleReadCapacity(EnableScalingProps.builder()
				                                      .minCapacity(1)
				                                      .maxCapacity(4)
				                                      .build())
				          .scaleOnUtilization(UtilizationScalingProps.builder().targetUtilizationPercent(50)
						                                                       .scaleInCooldown(Duration.seconds(30))
						                                                       .scaleOutCooldown(Duration.seconds(30))
						                                                       .build());
		
		
		eventsTable.autoScaleWriteCapacity(EnableScalingProps.builder()
				                                                   .minCapacity(1)
				                                                   .maxCapacity(4)
				                                                   .build())
		                  .scaleOnUtilization(UtilizationScalingProps.builder().targetUtilizationPercent(50)
				                                                               .scaleInCooldown(Duration.seconds(30))
				                                                               .scaleOutCooldown(Duration.seconds(30))
				                                                               .build());
		
		eventsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
				.indexName("emailIndex")
				.partitionKey(Attribute.builder().name("email").type(AttributeType.STRING).build())
				.sortKey(Attribute.builder().name("sk").type(AttributeType.STRING).build())
				.projectionType(ProjectionType.ALL).build());
		
		eventsTable.grantWriteData(ecommerceCommons.getProductEventFunction());
		
		
		this.grantPutItemAndQueryActionTable(eventsTable, ecommerceCommons);
		
	}
	
	private void grantPutItemAndQueryActionTable(Table eventsTable, EcommerceCommons ecommerceCommons) {
		
		ecommerceCommons.getOrdersEventFunction().addToRolePolicy(PolicyStatement.Builder.create()
				.effect(Effect.ALLOW)
				.actions(Arrays.asList("dynamodb:PutItem"))
				.resources(Arrays.asList(eventsTable.getTableArn()))
				.conditions(this.createConditions("#order_*"))
				.build());
		
		
		ecommerceCommons.getInvoiceEventFunction().addToRolePolicy(PolicyStatement.Builder.create()
				.effect(Effect.ALLOW)
				.actions(Arrays.asList("dynamodb:PutItem"))
				.resources(Arrays.asList(eventsTable.getTableArn()))
				.conditions(this.createConditions("#invoice_*"))
				.build());
		
		
		// Permissão para que a função faça apenas query utilizando um index específico
		ecommerceCommons.getOrdersEventFetchFunction().addToRolePolicy(PolicyStatement.Builder.create()
				.effect(Effect.ALLOW)
				.actions(Arrays.asList("dynamodb:Query"))
				.resources(Arrays.asList(eventsTable.getTableArn() + "/index/emailIndex"))
				.build());
		
	}
	
	private final Map<String, Map<String,List<String>>> createConditions(String key) {
		
		// Permissão de dar uma única ação de put item no dynamodb ao invés de dar permissão de escrita que libera várias ações
		final Map<String,List<String>> fieldStringRestriction = new HashMap<String, List<String>>();
		fieldStringRestriction.put("dynamodb:LeadingKeys", Arrays.asList(key));
		
		final Map<String, Map<String,List<String>>> conditions = new HashMap<>();
		conditions.put("ForAllValues:StringLike", fieldStringRestriction);
		
		return conditions;
	}
	
	private void createOrderTable(EcommerceCommons ecommerceCommons) {
		
		ecommerceCommons.setOrderTable(Table.Builder.create(this, "OrderTable")
				.tableName(TABLE_ORDER)
				.readCapacity(1)
				.writeCapacity(1)
				.billingMode(BillingMode.PROVISIONED)
				.partitionKey(Attribute.builder().name("pk").type(AttributeType.STRING).build())
				.sortKey(Attribute.builder().name("sk").type(AttributeType.STRING).build())
				.timeToLiveAttribute("ttl")
				.removalPolicy(RemovalPolicy.DESTROY).build());

		ecommerceCommons.getOrderTable().autoScaleReadCapacity(EnableScalingProps.builder()
				                                      .minCapacity(1)
				                                      .maxCapacity(4)
				                                      .build())
				          .scaleOnUtilization(UtilizationScalingProps.builder().targetUtilizationPercent(50)
						                                                       .scaleInCooldown(Duration.seconds(30))
						                                                       .scaleOutCooldown(Duration.seconds(30))
						                                                       .build());
		
		
		ecommerceCommons.getOrderTable().autoScaleWriteCapacity(EnableScalingProps.builder()
				                                                   .minCapacity(1)
				                                                   .maxCapacity(4)
				                                                   .build())
		                  .scaleOnUtilization(UtilizationScalingProps.builder().targetUtilizationPercent(50)
				                                                               .scaleInCooldown(Duration.seconds(30))
				                                                               .scaleOutCooldown(Duration.seconds(30))
				                                                               .build());
		
		ecommerceCommons.getOrderTable().grantReadWriteData(ecommerceCommons.getOrdersFunction());
	}
	
	
	private void createInvoiceTable(EcommerceCommons ecommerceCommons) {
		
		final Table invoiceTable = Table.Builder.create(this, "InvoiceTable")
				.tableName(TABLE_INVOICE)
				.readCapacity(1)
				.writeCapacity(1)
				.billingMode(BillingMode.PROVISIONED)
				.partitionKey(Attribute.builder().name("pk").type(AttributeType.STRING).build())
				.sortKey(Attribute.builder().name("sk").type(AttributeType.STRING).build())
				.timeToLiveAttribute("ttl")
				.stream(StreamViewType.NEW_AND_OLD_IMAGES)
				.removalPolicy(RemovalPolicy.DESTROY).build();
		
		  invoiceTable.grantWriteData(ecommerceCommons.getInvoiceGetUrlFunction());
		  invoiceTable.grantReadWriteData(ecommerceCommons.getInvoiceImportFunction());
		  invoiceTable.grantReadWriteData(ecommerceCommons.getInvoiceCancelImportFunction());
		  
          // Adiciona a função de invoices como fonte de eventos de atualizações do dynamodb
		   ecommerceCommons.getInvoiceEventFunction().addEventSource(DynamoEventSource.Builder.create(invoiceTable)
						                      .startingPosition(StartingPosition.TRIM_HORIZON) // Começa a ler o streams do último evento enviado pelo dynamo
						                      .batchSize(5) // Deixa acumular 5 eventos de atualizações antes de invocar a função lambda com as atualizações
						                      .bisectBatchOnError(true) // Se der algum dentro da função referente ao lote de atualizações reevie as atualizações que não obtiveram sucesso
						                      .enabled(true) 
						                      .onFailure(new SqsDlq(Queue.Builder.create(this, "InvoiceEventsDlqQueue")
						           		  		   .queueName("invoice-events-dlq-queue")
						        		  		   .enforceSsl(false)
						        		           .retentionPeriod(Duration.days(10))
						        		  	       .encryption(QueueEncryption.UNENCRYPTED)
						        		  		   .build()))
						                      .retryAttempts(3)
						                      .build());
		
	}

}
