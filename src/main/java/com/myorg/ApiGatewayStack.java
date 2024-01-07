package com.myorg;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.AccessLogFormat;
import software.amazon.awscdk.services.apigateway.AuthorizationType;
import software.amazon.awscdk.services.apigateway.CognitoUserPoolsAuthorizer;
import software.amazon.awscdk.services.apigateway.CognitoUserPoolsAuthorizerProps;
import software.amazon.awscdk.services.apigateway.IModel;
import software.amazon.awscdk.services.apigateway.IRequestValidator;
import software.amazon.awscdk.services.apigateway.JsonSchema;
import software.amazon.awscdk.services.apigateway.JsonSchemaType;
import software.amazon.awscdk.services.apigateway.JsonWithStandardFieldProps;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.LogGroupLogDestination;
import software.amazon.awscdk.services.apigateway.MethodOptions;
import software.amazon.awscdk.services.apigateway.Model;
import software.amazon.awscdk.services.apigateway.ModelProps;
import software.amazon.awscdk.services.apigateway.RequestValidator;
import software.amazon.awscdk.services.apigateway.RequestValidatorProps;
import software.amazon.awscdk.services.apigateway.Resource;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.apigateway.RestApiProps;
import software.amazon.awscdk.services.apigateway.StageOptions;
import software.amazon.awscdk.services.apigatewayv2.HttpMethod;
import software.amazon.awscdk.services.cognito.AccountRecovery;
import software.amazon.awscdk.services.cognito.AuthFlow;
import software.amazon.awscdk.services.cognito.AutoVerifiedAttrs;
import software.amazon.awscdk.services.cognito.CognitoDomainOptions;
import software.amazon.awscdk.services.cognito.OAuthScope;
import software.amazon.awscdk.services.cognito.OAuthSettings;
import software.amazon.awscdk.services.cognito.PasswordPolicy;
import software.amazon.awscdk.services.cognito.ResourceServerScope;
import software.amazon.awscdk.services.cognito.ResourceServerScopeProps;
import software.amazon.awscdk.services.cognito.SignInAliases;
import software.amazon.awscdk.services.cognito.StandardAttribute;
import software.amazon.awscdk.services.cognito.StandardAttributes;
import software.amazon.awscdk.services.cognito.UserInvitationConfig;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.cognito.UserPoolClientOptions;
import software.amazon.awscdk.services.cognito.UserPoolDomainOptions;
import software.amazon.awscdk.services.cognito.UserPoolProps;
import software.amazon.awscdk.services.cognito.UserPoolResourceServer;
import software.amazon.awscdk.services.cognito.UserPoolResourceServerOptions;
import software.amazon.awscdk.services.cognito.UserPoolTriggers;
import software.amazon.awscdk.services.cognito.UserVerificationConfig;
import software.amazon.awscdk.services.cognito.VerificationEmailStyle;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.Policy;
import software.amazon.awscdk.services.iam.PolicyProps;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.LambdaInsightsVersion;
import software.amazon.awscdk.services.lambda.Tracing;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.constructs.Construct;

public class ApiGatewayStack extends Stack implements BaseDockerBuild {
	
	private CognitoUserPoolsAuthorizer productsAdminAuthorizer;
	
	private CognitoUserPoolsAuthorizer productsAuthorizer;
	
	private UserPool custumerPool;
	
	private static final String WEB_SCOPE = "web";
	
	private static final String MOBILE_SCOPE = "mobile";
	
	private static final String ADMIN_RESOURCE_SERVER = "admin";
	
	private static final String CUSTUMER_RESOURCE_SERVER = "custumer";
	
	private UserPool adminPool;
	
	private Function postConfirmationFunction;
	
	private Function preAuthenticationFunction;
 

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
		 
		 
		
		 
		 this.createCognitoAuth(ecommerceCommons);
		 this.createApiProducts(ecommerceCommons, restApi);
		 this.createApiOrders(ecommerceCommons, restApi);
		 
 
 
	}
	
	private void crateLambdaAuthenticationTriggers() {
		
		this.postConfirmationFunction  =  new Function(this, "PostConfirmationFunction", FunctionProps.builder()
	                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
	                .functionName("PostConfirmationFunction")
	                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
	                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
	                        .build()))
	                .handler("com.br.aws.ecommerce.auth.PostConfirmationFunction")
	                .memorySize(256)
	                .tracing(Tracing.ACTIVE)
	                .insightsVersion(LambdaInsightsVersion.VERSION_1_0_119_0)
	                .timeout(Duration.seconds(10))
	                .logRetention(RetentionDays.ONE_WEEK)
	                .build());
		
		
		this.preAuthenticationFunction  =  new Function(this, "PreAuthenticationFunction", FunctionProps.builder()
                .runtime(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME)
                .functionName("PreAuthenticationFunction")
                .code(Code.fromAsset("../" + AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME + "/", AssetOptions.builder()
                        .bundling(getBundlingOptions(AwsLambdaCdkApp.PROJECT_LAMBDA_FUNCTIONS_NAME))
                        .build()))
                .handler("com.br.aws.ecommerce.auth.PreAuthenticationFunction")
                .memorySize(256)
                .tracing(Tracing.ACTIVE)
                .insightsVersion(LambdaInsightsVersion.VERSION_1_0_119_0)
                .timeout(Duration.seconds(10))
                .logRetention(RetentionDays.ONE_WEEK)
                .build());
	                
	    
	}
	
	private void createAdminPool(EcommerceCommons ecommerceCommons) {
		
		// FORCE_CHANGE_PASSWORD 
		this.adminPool = new UserPool(this, "AdminPool", 
				UserPoolProps.builder()
				.userPoolName("AdminPool")
				.removalPolicy(RemovalPolicy.DESTROY)
				.selfSignUpEnabled(false)
			    .userInvitation(UserInvitationConfig
			    		.builder()
			    		.emailSubject("Welcome to administrator area!")
			    		.emailBody("Your username is {username} and your password is {####} "
			    				+ "<p><strong><font size=\"15\">O velho boca de c*!</font></strong>"
			    				+ "<p> Please <a href=\"url_login\">sign in</a> ")
			    		.build())
				.signInAliases(SignInAliases
						.builder()
						.email(true)
						.username(false)
						.build())
				.standardAttributes(StandardAttributes
								.builder()
								.email(StandardAttribute
										   .builder()
										   .required(true)
										   .mutable(false) // Não pode alterar
										   .build())
								.build()) // Na página de registro é obrigatório digitar o nome completo
				.passwordPolicy(PasswordPolicy
						.builder()
						.minLength(8)
						.requireLowercase(true)
						.requireUppercase(true)
						.requireDigits(true)
						.requireSymbols(true)
						.build())
				.accountRecovery(AccountRecovery.EMAIL_ONLY)
				.build());
		
		this.adminPool.addDomain("AdminDomain",
				UserPoolDomainOptions.builder()
						.cognitoDomain(CognitoDomainOptions
								        .builder()
								        .domainPrefix("500as-admin-service")
								        .build())
						.build());
		
		final ResourceServerScope adminWebScope = new ResourceServerScope(ResourceServerScopeProps.builder()
				.scopeName(WEB_SCOPE).scopeDescription("Admin web operation").build());
		
		final UserPoolResourceServer adminUserPoolRS = this.adminPool.addResourceServer("AdminResourceServer",
				UserPoolResourceServerOptions
				.builder()
				.userPoolResourceServerName("AdminResourceServer")
				.identifier(ADMIN_RESOURCE_SERVER)
				.scopes(Arrays.asList(adminWebScope))
				.build());
		
		
		this.adminPool.addClient("admin-web-client", UserPoolClientOptions
				.builder()
				.userPoolClientName("adminWebClient")
				.authFlows(AuthFlow.builder().userPassword(true).build())
				.accessTokenValidity(Duration.minutes(10))
				.refreshTokenValidity(Duration.hours(1))
				.oAuth(OAuthSettings.builder().scopes(Arrays.asList(OAuthScope.resourceServer(adminUserPoolRS, adminWebScope))). build() )
				.build());
		
		
		this.productsAdminAuthorizer = new CognitoUserPoolsAuthorizer(this, "ProductsAdminAuthorizer", CognitoUserPoolsAuthorizerProps
				.builder()
				.cognitoUserPools(Arrays.asList(this.adminPool))
				.build());
		
		;
		
		final Policy adminGetUserPolicy =  new Policy(this, "AdminGetUserPolicy", PolicyProps.builder().statements(Arrays.asList(PolicyStatement.Builder.create()
				                                                                                                   .effect(Effect.ALLOW)
				                                                                                                   .actions(Arrays.asList("*"))
				                                                                                                   .resources(Arrays.asList(this.adminPool.getUserPoolArn()))
				                                                                                                   .build()))
				                                                  .build());
		
		adminGetUserPolicy.attachToRole(ecommerceCommons.getProductsAdminFunction().getRole());
		
		
		// Adiciona a permissão para que a função lambda busca informações do usuário logado no cognito
	//	ecommerceCommons.getProductsAdminFunction().addToRolePolicy(PolicyStatement.Builder.create()
	//			.effect(Effect.ALLOW)
				//.actions(Arrays.asList("cognito-idp:AdminGetUser"))
		//		.actions(Arrays.asList("*"))
		//		.resources(Arrays.asList(this.adminPool.getUserPoolArn()))
		//		.build());
		
		
		
		
		
	}
	
	private void createCustumerPool() {
		
		this.crateLambdaAuthenticationTriggers();
		
		this.custumerPool = new UserPool(this, "CustumerPool", 
				UserPoolProps.builder()
				.userPoolName("CustumerPool")
				.removalPolicy(RemovalPolicy.DESTROY)
				.selfSignUpEnabled(true) // Usuário pode se auto registrar no user pool, exemplo uma página de cadastro para o usuário
				.autoVerify(AutoVerifiedAttrs.builder()
						                     .phone(false)
						                     .email(true)
						                     .build()) // Manda um email para confirmação de cadastro 
						.userVerification(UserVerificationConfig.builder()
						.emailSubject("Verify your email for te Ecommerce Service!")
						.emailBody("Thanks for the registration, your verification code is {####}")
						.emailStyle(VerificationEmailStyle.CODE)
						.build())
				.signInAliases(SignInAliases
						.builder()
						.email(true)
						.username(false)
						.build())
				.standardAttributes(StandardAttributes
						.builder()
						.fullname(StandardAttribute
								   .builder()
								   .required(true)
								   .mutable(false) // Não pode alterar
								   .build())
						.build()) // Na página de registro é obrigatório digitar o nome completo
				.passwordPolicy(PasswordPolicy
						.builder()
						.minLength(8)
						.requireLowercase(true)
						.requireUppercase(true)
						.requireDigits(true)
						.requireSymbols(true)
						.build())
						.lambdaTriggers(UserPoolTriggers
								          .builder()
								          .preAuthentication(this.preAuthenticationFunction)
								          .postConfirmation(this.postConfirmationFunction) // Depois da confirmação de criação do usuário
								          .build())
				.accountRecovery(AccountRecovery.EMAIL_ONLY)
				.build());
		
		this.custumerPool.addDomain("CustumerDomain",
				UserPoolDomainOptions.builder()
						.cognitoDomain(CognitoDomainOptions
								        .builder()
								        .domainPrefix("500as-custumer-service")
								        .build())
						.build());
		
		
		final ResourceServerScope custemerWebScope = new ResourceServerScope(ResourceServerScopeProps.builder()
				.scopeName(WEB_SCOPE).scopeDescription("Custumer web operation").build());
		
		final ResourceServerScope custumerMobileScope = new ResourceServerScope(ResourceServerScopeProps.builder()
				.scopeName(MOBILE_SCOPE).scopeDescription("Custumer mobile operation").build());
		
		final UserPoolResourceServer custerUserPoolRS = this.custumerPool.addResourceServer("CustumerResourceServer",
				UserPoolResourceServerOptions
				.builder()
				.userPoolResourceServerName("CustumerResourceServer")
				.identifier(CUSTUMER_RESOURCE_SERVER)
				.scopes(Arrays.asList(custemerWebScope, custumerMobileScope))
				.build()); // Clientes acessando por ambos os escops (mobile ou web) terão acesso a todos os recursos liberados pelo resource server

		 
		
		this.custumerPool.addClient("custumer-web-client", UserPoolClientOptions
				.builder()
				.userPoolClientName("custumerWebClient")
				.authFlows(AuthFlow.builder().userPassword(true).build())
				.accessTokenValidity(Duration.minutes(10))
				.refreshTokenValidity(Duration.hours(1))
				.oAuth(OAuthSettings.builder().scopes(Arrays.asList(OAuthScope.resourceServer(custerUserPoolRS, custemerWebScope))). build() )
				.build());
		
		
		this.custumerPool.addClient("custumer-mobile-client", UserPoolClientOptions
				.builder()
				.userPoolClientName("custumerMobileClient")
				.authFlows(AuthFlow.builder().userPassword(true).build())
				.accessTokenValidity(Duration.minutes(10))
				.refreshTokenValidity(Duration.hours(1))
				.oAuth(OAuthSettings.builder().scopes(Arrays.asList(OAuthScope.resourceServer(custerUserPoolRS, custumerMobileScope))). build() )
				.build());
		
	}
	
	private void createCognitoAuth(EcommerceCommons ecommerceCommons) {	
		
	  	this.createCustumerPool();
		
		this.createAdminPool(ecommerceCommons);
	
		this.productsAuthorizer = new CognitoUserPoolsAuthorizer(this, "ProductsAuthorizer", CognitoUserPoolsAuthorizerProps
				.builder()
				.cognitoUserPools(Arrays.asList(this.custumerPool, this.adminPool))
				.build());
	}
	
	private void createApiProducts(EcommerceCommons ecommerceCommons, RestApi restApi) {
		

		
         final LambdaIntegration lambdaProductFetchIntegration = LambdaIntegration.Builder.create(ecommerceCommons.getProductsFetchFunction()).build();
		 
         
     //    final MethodOptions productsFetchMobileIntegrationOption =  MethodOptions
      //  		 .builder()
       // 		 .authorizer(productsAuthorizer)
       // 		 .authorizationType(AuthorizationType.COGNITO)
       // 		 .authorizationScopes(Arrays.asList(CUSTUMER_RESOURCE_SERVER + WEB_SCOPE, CUSTUMER_RESOURCE_SERVER + "/" + MOBILE_SCOPE,
       // 				 ADMIN_RESOURCE_SERVER + "/" + WEB_SCOPE))
       // 		 .build();
         
         
         final MethodOptions productsFetchWebIntegrationOption =  MethodOptions
        		 .builder()
        		 .authorizer(productsAuthorizer)
        		 .authorizationType(AuthorizationType.COGNITO)
        		 .authorizationScopes(Arrays.asList(CUSTUMER_RESOURCE_SERVER + "/" + WEB_SCOPE,
        				 ADMIN_RESOURCE_SERVER + "/" + WEB_SCOPE))
        		 .build();
         
         final MethodOptions productsAdminWebIntegrationOption =  MethodOptions
        		 .builder()
        		 .authorizer(productsAdminAuthorizer)
        		 .authorizationType(AuthorizationType.COGNITO)
        		 .authorizationScopes(Arrays.asList(ADMIN_RESOURCE_SERVER + "/" + WEB_SCOPE))
        		 .build();
         
		 final Resource productResource =  restApi.getRoot().addResource("products");
		// productResource.addMethod(HttpMethod.GET.toString(), lambdaProductFetchIntegration, productsFetchMobileIntegrationOption);
		 
		 productResource.addMethod(HttpMethod.GET.toString(), lambdaProductFetchIntegration, productsFetchWebIntegrationOption);
		 
		 
		 final Resource productIdResource = productResource.addResource("{id}");
		 productIdResource.addMethod(HttpMethod.GET.toString(), lambdaProductFetchIntegration, productsFetchWebIntegrationOption);
		 
		 final LambdaIntegration lambdaProductAdminIntegration = LambdaIntegration.Builder.create(ecommerceCommons.getProductsAdminFunction()).build();
		 
		 productResource.addMethod(HttpMethod.POST.toString(), lambdaProductAdminIntegration, productsAdminWebIntegrationOption);
		 productIdResource.addMethod(HttpMethod.PUT.toString(), lambdaProductAdminIntegration, productsAdminWebIntegrationOption);
		 productIdResource.addMethod(HttpMethod.DELETE.toString(), lambdaProductAdminIntegration, productsAdminWebIntegrationOption);
		
	}
	
	private void createApiOrders(EcommerceCommons ecommerceCommons, RestApi restApi) {
		
		 final LambdaIntegration lambdaOrderIntegration = LambdaIntegration.Builder.create(ecommerceCommons.getOrdersFunction()).build();
		 final Resource orderResource =  restApi.getRoot().addResource("orders");
		 
		 final Map<String, Boolean> requiredParameters = new HashMap<>();
		 requiredParameters.put("method.request.querystring.email", true);
		 requiredParameters.put("method.request.querystring.orderId", true);
		 
		final IRequestValidator deleteValidators = new RequestValidator(this, "OrderDeletionValidator",
					RequestValidatorProps.builder()
					.requestValidatorName("OrderDeletionValidator")
					.restApi(restApi)
					.validateRequestParameters(true)
					.build());
		
		final IRequestValidator postValidators = new RequestValidator(this, "OrderPostValidator",
				RequestValidatorProps.builder()
				.requestValidatorName("OrderPostValidator")
				.restApi(restApi)
		        .validateRequestBody(true)
				.build());
		
		
		final Map<String, JsonSchema> mapPropertiesBilling = new HashMap<>();
		mapPropertiesBilling.put("payment", JsonSchema.builder().type(JsonSchemaType.STRING)
				                                   .enumValue(Arrays.asList("CARD", "PIX", "CASH"))
				                                   .build());
		
		final Map<String, JsonSchema> mapPropertiesOrder = new HashMap<>();
		mapPropertiesOrder.put("pk", JsonSchema.builder().type(JsonSchemaType.STRING).build());
		mapPropertiesOrder.put("shipping", JsonSchema.builder().type(JsonSchemaType.OBJECT).properties(mapPropertiesBilling).build());
		mapPropertiesOrder.put("idsProducts", JsonSchema.builder()
				                                   .type(JsonSchemaType.ARRAY)
				                                   .minItems(1)
				                                   .items(JsonSchema.builder()
				                                         .type(JsonSchemaType.STRING)
				                                         .build())
				                                 .build());
		
		final Model mapOrderModel = new Model(this, "OrderModel", ModelProps
				.builder()
				.restApi(restApi)
				.schema(JsonSchema.builder()
						           .type(JsonSchemaType.OBJECT)
						           .properties(mapPropertiesOrder)
						           .required(Arrays.asList("pk", "idsProducts"))
						           .build())
				.build());
		
		final Map<String, IModel> mapModels = new HashMap<>();
		mapModels.put("application/json", mapOrderModel);

		 
		 orderResource.addMethod(HttpMethod.POST.toString(), lambdaOrderIntegration, MethodOptions.builder().requestModels(mapModels)
				 .requestValidator(postValidators).build());
		 orderResource.addMethod(HttpMethod.DELETE.toString(), lambdaOrderIntegration, MethodOptions.builder().requestParameters(requiredParameters)
				 .requestValidator(deleteValidators).build());
		 orderResource.addMethod(HttpMethod.GET.toString(), lambdaOrderIntegration);
		 
		 this.createApiOrdersEventsFetch(ecommerceCommons, restApi, orderResource);
		 
		 
		 
		
	}
	
	private void createApiOrdersEventsFetch(EcommerceCommons ecommerceCommons, RestApi restApi,  final Resource orderResource) {
		
		final Resource eventResource =  orderResource.addResource("events");
		
		
		 final LambdaIntegration lambdaOrderEventsFetchIntegration = LambdaIntegration.Builder.create(ecommerceCommons.getOrdersEventFetchFunction()).build();
		
		 final Map<String, Boolean> requiredParameters = new HashMap<>();
		 requiredParameters.put("method.request.querystring.email", true);
		 requiredParameters.put("method.request.querystring.eventType", false);
		 
		 final IRequestValidator requestValidators = new RequestValidator(this, "OrderEventFetchValidator",
					RequestValidatorProps.builder()
					.requestValidatorName("OrderEventFetchValidator")
					.restApi(restApi)
					.validateRequestParameters(true)
					.build());
			
			
		  eventResource.addMethod(HttpMethod.GET.toString(), lambdaOrderEventsFetchIntegration, MethodOptions.builder().requestParameters(requiredParameters)
					 .requestValidator(requestValidators).build());
		 
		
	}
	
	
    

}
