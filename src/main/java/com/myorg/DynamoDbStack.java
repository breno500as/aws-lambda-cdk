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
	
	private Table productTable;
	
	public static final String TABLE_PRODUCT = "product";
	
	
	public DynamoDbStack(final Construct scope, final String id, ProductCommons productCommonsStack, StackProps stackProps) {
		super(scope, id, stackProps);
		
		this.productTable = Table.Builder.create(this, "ProductTable")
				.tableName(TABLE_PRODUCT)
				.readCapacity(1)
				.writeCapacity(1)
				.billingMode(BillingMode.PROVISIONED)
				.partitionKey(Attribute.builder().name("id").type(AttributeType.STRING).build())
				.timeToLiveAttribute("ttl")
				.removalPolicy(RemovalPolicy.DESTROY).build();

		this.productTable.autoScaleReadCapacity(EnableScalingProps.builder()
				                                      .minCapacity(1)
				                                      .maxCapacity(4)
				                                      .build())
				          .scaleOnUtilization(UtilizationScalingProps.builder().targetUtilizationPercent(50)
						                                                       .scaleInCooldown(Duration.seconds(30))
						                                                       .scaleOutCooldown(Duration.seconds(30))
						                                                       .build());
		
		
		this.productTable.autoScaleWriteCapacity(EnableScalingProps.builder()
				                                                   .minCapacity(1)
				                                                   .maxCapacity(4)
				                                                   .build())
		                  .scaleOnUtilization(UtilizationScalingProps.builder().targetUtilizationPercent(50)
				                                                               .scaleInCooldown(Duration.seconds(30))
				                                                               .scaleOutCooldown(Duration.seconds(30))
				                                                               .build());
		
		this.productTable.grantReadData(productCommonsStack.getProductsFetchFunction());
		this.productTable.grantWriteData(productCommonsStack.getProductsAdminFunction());
	}
	
	
	
	public Table getProductTable() {
		return productTable;
	}
	


}
