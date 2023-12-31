package com.myorg;

import java.util.HashMap;
import java.util.Map;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.AccessLogFormat;
import software.amazon.awscdk.services.apigateway.IRequestValidator;
import software.amazon.awscdk.services.apigateway.JsonWithStandardFieldProps;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.LogGroupLogDestination;
import software.amazon.awscdk.services.apigateway.MethodOptions;
import software.amazon.awscdk.services.apigateway.RequestValidator;
import software.amazon.awscdk.services.apigateway.RequestValidatorProps;
import software.amazon.awscdk.services.apigateway.Resource;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.apigateway.RestApiProps;
import software.amazon.awscdk.services.apigateway.StageOptions;
import software.amazon.awscdk.services.apigatewayv2.HttpMethod;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;

public class ApiGatewayStack  extends Stack {
	
	
 

	public ApiGatewayStack(final Construct scope, final String id, EcommerceCommons ecommerceCommons, StackProps stackProps) {
		
		 super(scope, id, stackProps);
		 
		 final RestApi restApi = new RestApi(this, "ecommerce-apigateway", 
				 RestApiProps.builder()
				 .restApiName("ecommerce-apigateway")
				 .cloudWatchRole(true)
				 .deployOptions(StageOptions.builder()
						        .accessLogDestination(new LogGroupLogDestination(LogGroup.Builder.create(this, "ECommerceApiLogs")
							                                                                      .logGroupName("ECommerceApiLogs")
							                                                                      .removalPolicy(RemovalPolicy.DESTROY)
							                                                                      .build()))
						        .tracingEnabled(Boolean.TRUE)
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
		 
		 
		
		 
		 
		 this.createApiProducts(ecommerceCommons, restApi);
		 this.createApiOrders(ecommerceCommons, restApi);
		 
		 
		// Exporta como parâmetro a url do API Gateway
		//  new CfnOutput(this, "HttApiProductsFetch", CfnOutputProps.builder()
		//           .description("Url for Http Api Products Ecommerce")
		 //           .value(restApi.getUrl())
		  //          .build()); 
 
	}
	
	private void createApiProducts(EcommerceCommons ecommerceCommons, RestApi restApi) {
		
         final LambdaIntegration lambdaProductFetchIntegration = LambdaIntegration.Builder.create(ecommerceCommons.getProductsFetchFunction()).build();
		 
		 final Resource productResource =  restApi.getRoot().addResource("products");
		 productResource.addMethod(HttpMethod.GET.toString(), lambdaProductFetchIntegration);
		 
		 final Resource productIdResource = productResource.addResource("{id}");
		 productIdResource.addMethod(HttpMethod.GET.toString(), lambdaProductFetchIntegration);
		 
		 final LambdaIntegration lambdaProductAdminIntegration = LambdaIntegration.Builder.create(ecommerceCommons.getProductsAdminFunction()).build();
		 
		 productResource.addMethod(HttpMethod.POST.toString(), lambdaProductAdminIntegration);
		 productIdResource.addMethod(HttpMethod.PUT.toString(), lambdaProductAdminIntegration);
		 productIdResource.addMethod(HttpMethod.DELETE.toString(), lambdaProductAdminIntegration);
		
	}
	
	private void createApiOrders(EcommerceCommons ecommerceCommons, RestApi restApi) {
		
		 final LambdaIntegration lambdaOrderIntegration = LambdaIntegration.Builder.create(ecommerceCommons.getOrdersFunction()).build();
		 final Resource orderResource =  restApi.getRoot().addResource("orders");
		 
		 Map<String, Boolean> requiredParameters = new HashMap<>();
		 requiredParameters.put("method.request.querystring.email", true);
		 requiredParameters.put("method.request.querystring.orderId", true);
		 
		final IRequestValidator validator = new RequestValidator(this, "OrderDeletionValidator",
					RequestValidatorProps.builder()
					.requestValidatorName("OrderDeletionValidator")
					.restApi(restApi)
					.validateRequestParameters(true)
					.build());

		 orderResource.addMethod(HttpMethod.GET.toString(), lambdaOrderIntegration);
		 orderResource.addMethod(HttpMethod.POST.toString(), lambdaOrderIntegration);
		 orderResource.addMethod(HttpMethod.DELETE.toString(), lambdaOrderIntegration, MethodOptions.builder().requestParameters(requiredParameters)
				 .requestValidator(validator).build());
		
	}
	
	
    

}
