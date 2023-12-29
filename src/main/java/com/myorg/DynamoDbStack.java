package com.myorg;

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
import software.constructs.Construct;

public class DynamoDbStack  extends Stack {
	
 
	
	public static final String EVENTS_DDB = "EVENTS_DDB";
	
	public static final String TABLE_PRODUCT = "product";
	
	public static final String TABLE_EVENT = "event";
	
	
	public DynamoDbStack(final Construct scope, final String id, EcommerceCommons ecommerceCommons, StackProps stackProps) {
		super(scope, id, stackProps);
	
		this.createProductTable(ecommerceCommons);
	    this.createEventsTable(ecommerceCommons);
		
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
	}

}
