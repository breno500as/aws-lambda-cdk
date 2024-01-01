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
	
	
	public DynamoDbStack(final Construct scope, final String id, EcommerceCommons ecommerceCommons, StackProps stackProps) {
		super(scope, id, stackProps);
	
		this.createProductTable(ecommerceCommons);
	    this.createEventsTable(ecommerceCommons);
	    this.createOrderTable(ecommerceCommons);
		
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
		
		eventsTable.grantWriteData(ecommerceCommons.getEventsFunction());
		this.grantSpecifcActionTable(eventsTable, ecommerceCommons);
		
	}
	
	private void grantSpecifcActionTable(Table eventsTable, EcommerceCommons ecommerceCommons) {
		
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
		
	}
	
	private void createOrderTable(EcommerceCommons ecommerceCommons) {
		
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
