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

public class EventsDynamoDbStack extends Stack {
	
private Table eventsTable;
	
	public static final String TABLE_EVENTS = "events";
	
	
	public EventsDynamoDbStack(final Construct scope, final String id, ProductCommons productCommonsStack, StackProps stackProps) {
		super(scope, id, stackProps);
		
		this.eventsTable = Table.Builder.create(this, "EventsTable")
				.tableName(TABLE_EVENTS)
				.readCapacity(1)
				.writeCapacity(1)
				.billingMode(BillingMode.PROVISIONED)
				.partitionKey(Attribute.builder().name("pk").type(AttributeType.STRING).build())
				.sortKey(Attribute.builder().name("sk").type(AttributeType.STRING).build())
				.timeToLiveAttribute("ttl")
				.removalPolicy(RemovalPolicy.DESTROY).build();

		this.eventsTable.autoScaleReadCapacity(EnableScalingProps.builder()
				                                      .minCapacity(1)
				                                      .maxCapacity(4)
				                                      .build())
				          .scaleOnUtilization(UtilizationScalingProps.builder().targetUtilizationPercent(50)
						                                                       .scaleInCooldown(Duration.seconds(30))
						                                                       .scaleOutCooldown(Duration.seconds(30))
						                                                       .build());
		
		
		this.eventsTable.autoScaleWriteCapacity(EnableScalingProps.builder()
				                                                   .minCapacity(1)
				                                                   .maxCapacity(4)
				                                                   .build())
		                  .scaleOnUtilization(UtilizationScalingProps.builder().targetUtilizationPercent(50)
				                                                               .scaleInCooldown(Duration.seconds(30))
				                                                               .scaleOutCooldown(Duration.seconds(30))
				                                                               .build());
		
		this.eventsTable.grantReadData(productCommonsStack.getProductsFetchFunction());
		this.eventsTable.grantWriteData(productCommonsStack.getProductsAdminFunction());
	}
	
	
	
	public Table getEventsTable() {
		return eventsTable;
	}

}
