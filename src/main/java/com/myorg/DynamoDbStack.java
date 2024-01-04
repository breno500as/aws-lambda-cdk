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
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.UtilizationScalingProps;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.constructs.Construct;

public class DynamoDbStack  extends Stack {
	
	public static final String PRODUCTS_DDB = "PRODUCTS_DDB";
	
	public static final String ORDERS_DDB = "ORDERS_DDB";

	public static final String EVENTS_DDB = "EVENTS_DDB";

	public static final String TABLE_PRODUCT = "product";

	public static final String TABLE_EVENT = "event";

	public static final String TABLE_ORDER = "order";
	
	
	public DynamoDbStack(final Construct scope, final String id, EcommerceFunctionCommons ecommerceCommons, StackProps stackProps) {
		super(scope, id, stackProps);
	
		this.createProductTable(ecommerceCommons);
	    this.createEventsTable(ecommerceCommons);
	    this.createOrderTable(ecommerceCommons);
		
	}
	
	
	private void createProductTable(EcommerceFunctionCommons ecommerceCommons) {
		
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
	
	private void createEventsTable(EcommerceFunctionCommons ecommerceCommons) {
	
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
	
	private void grantPutItemAndQueryActionTable(Table eventsTable, EcommerceFunctionCommons ecommerceCommons) {
		
		// Permissão de dar uma única ação de put item no dynamodb ao invés de dar permissão de escrita que libera várias ações
		final Map<String,List<String>> fieldStringRestriction = new HashMap<String, List<String>>();
		fieldStringRestriction.put("dynamodb:LeadingKeys", Arrays.asList("#order_*"));
		
		final Map<String, Map<String,List<String>>> conditions = new HashMap<>();
		conditions.put("ForAllValues:StringLike", fieldStringRestriction);
		
	 
	
		ecommerceCommons.getOrdersEventFunction().addToRolePolicy(PolicyStatement.Builder.create()
				.effect(Effect.ALLOW)
				.actions(Arrays.asList("dynamodb:PutItem"))
				.resources(Arrays.asList(eventsTable.getTableArn()))
				.conditions(conditions)
				.build());
		
		
		// Permissão para que a função faça apenas query utilizando um index específico
		ecommerceCommons.getOrdersEventFetchFunction().addToRolePolicy(PolicyStatement.Builder.create()
				.effect(Effect.ALLOW)
				.actions(Arrays.asList("dynamodb:Query"))
				.resources(Arrays.asList(eventsTable.getTableArn() + "/index/emailIndex"))
				.build());
		
	}
	
	private void createOrderTable(EcommerceFunctionCommons ecommerceCommons) {
		
		final Table orderTable = Table.Builder.create(this, "OrderTable")
				.tableName(TABLE_ORDER)
				.readCapacity(1)
				.writeCapacity(1)
				.billingMode(BillingMode.PROVISIONED)
				.partitionKey(Attribute.builder().name("pk").type(AttributeType.STRING).build())
				.sortKey(Attribute.builder().name("sk").type(AttributeType.STRING).build())
				.timeToLiveAttribute("ttl")
				.removalPolicy(RemovalPolicy.DESTROY).build();

		orderTable.autoScaleReadCapacity(EnableScalingProps.builder()
				                                      .minCapacity(1)
				                                      .maxCapacity(4)
				                                      .build())
				          .scaleOnUtilization(UtilizationScalingProps.builder().targetUtilizationPercent(50)
						                                                       .scaleInCooldown(Duration.seconds(30))
						                                                       .scaleOutCooldown(Duration.seconds(30))
						                                                       .build());
		
		
		orderTable.autoScaleWriteCapacity(EnableScalingProps.builder()
				                                                   .minCapacity(1)
				                                                   .maxCapacity(4)
				                                                   .build())
		                  .scaleOnUtilization(UtilizationScalingProps.builder().targetUtilizationPercent(50)
				                                                               .scaleInCooldown(Duration.seconds(30))
				                                                               .scaleOutCooldown(Duration.seconds(30))
				                                                               .build());
		
		orderTable.grantReadWriteData(ecommerceCommons.getOrdersFunction());
	}

}
