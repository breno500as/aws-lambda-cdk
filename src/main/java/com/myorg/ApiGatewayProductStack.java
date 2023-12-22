package com.myorg;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.AccessLogFormat;
import software.amazon.awscdk.services.apigateway.JsonWithStandardFieldProps;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.LogGroupLogDestination;
import software.amazon.awscdk.services.apigateway.Resource;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.apigateway.RestApiProps;
import software.amazon.awscdk.services.apigateway.StageOptions;
import software.amazon.awscdk.services.apigatewayv2.HttpMethod;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;

public class ApiGatewayProductStack  extends Stack {
	
	
 

	public ApiGatewayProductStack(final Construct scope, final String id, ProductCommonsStack productCommonsStack, StackProps stackProps) {
		
		 super(scope, id, stackProps);
		 
		 final RestApi restApi = new RestApi(this, "products-ecommerce", 
				 RestApiProps.builder()
				 .restApiName("products-ecommerce")
				 .cloudWatchRole(true)
				 .deployOptions(StageOptions.builder()
						        .accessLogDestination(new LogGroupLogDestination(LogGroup.Builder.create(this, "ECommerceApiLogs")
							                                                                      .logGroupName("ECommerceApiLogs")
							                                                                      .removalPolicy(RemovalPolicy.DESTROY)
							                                                                      .build()))
						        .accessLogFormat(AccessLogFormat.jsonWithStandardFields(JsonWithStandardFieldProps.builder()
						        		                                                                          .httpMethod(true)
						        		                                                                          .ip(true)
						        		                                                                          .protocol(true)
						        		                                                                          .requestTime(true)
						        		                                                                          .resourcePath(true)
						        		                                                                          .responseLength(true)
						        		                                                                          .status(true)
						        		                                                                          .caller(true)
						        		                                                                          .user(true)
						        		                                                                          .build()))
						        .build())
				 .build());
		 
		 final Resource resource =  restApi.getRoot().addResource("products");
		 resource.addMethod(HttpMethod.GET.toString(), LambdaIntegration.Builder.create(productCommonsStack.getProductsFetchFunction()).build());
		 resource.addMethod(HttpMethod.POST.toString(), LambdaIntegration.Builder.create(productCommonsStack.getProductsAdminFunction()).build());
		 resource.addMethod(HttpMethod.PUT.toString(), LambdaIntegration.Builder.create(productCommonsStack.getProductsAdminFunction()).build());
		 resource.addMethod(HttpMethod.DELETE.toString(), LambdaIntegration.Builder.create(productCommonsStack.getProductsAdminFunction()).build());
		 
		// Exporta como parâmetro a url do API Gateway
		//  new CfnOutput(this, "HttApiProductsFetch", CfnOutputProps.builder()
		//           .description("Url for Http Api Products Ecommerce")
		 //           .value(restApi.getUrl())
		  //          .build());
		 
		 
 
	}
	
	
    

}
