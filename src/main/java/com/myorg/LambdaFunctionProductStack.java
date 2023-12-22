package com.myorg;

import static java.util.Collections.singletonList;
import static software.amazon.awscdk.BundlingOutput.ARCHIVED;

import java.util.Arrays;
import java.util.List;

import software.amazon.awscdk.BundlingOptions;
import software.amazon.awscdk.DockerVolume;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.constructs.Construct;




public class LambdaFunctionProductStack extends Stack {
	
	
	public LambdaFunctionProductStack(final Construct scope, final String id, ProductCommonsStack productCommonsStack, final StackProps props) {
		
		super(scope, id, props);
		
		   final List<String> functionPackagingInstructions = Arrays.asList(
			        "/bin/sh",
	                "-c",
	                "mvn clean install " +
	                "&& cp /asset-input/target/aws-ecommerce-lambda-functions.jar /asset-output/"
	        );
		 
		 
		    BundlingOptions.Builder builderOptions = BundlingOptions.builder()
	                .command(functionPackagingInstructions)
	                .image(Runtime.JAVA_11.getBundlingImage())
	                .volumes(singletonList(
	                        // Mount local .m2 repo to avoid download all the dependencies again inside the container
	                        DockerVolume.builder()
	                                .hostPath(System.getProperty("user.home") + "/.m2/")
	                                .containerPath("/root/.m2/")
	                                .build()
	                ))
	                .user("root")
	                .outputType(ARCHIVED);
		    
		    // Função 1 - ProductsFetchFunction
		    productCommonsStack.setProductsFetchFunction(new Function(this, "ProductsFetchFunction", FunctionProps.builder()
	                .runtime(Runtime.JAVA_11)
	                .code(Code.fromAsset("../aws-ecommerce-lambda-functions/", AssetOptions.builder()
	                        .bundling(builderOptions
	                                .command(functionPackagingInstructions)
	                                .build())
	                        .build()))
	                .handler("com.br.aws.ecommerce.product.ProductsFetchFunction")
	                .memorySize(512)
	                .timeout(Duration.seconds(10))
	                .logRetention(RetentionDays.ONE_WEEK)
	                .build()));
		    
		    
		    
		       
	        
	        // Função 2 - ProductsAdminFunction    
		    productCommonsStack.setProductsAdminFunction(new Function(this, "ProductsAdminFunction", FunctionProps.builder()
	                .runtime(Runtime.JAVA_11)
	                .code(Code.fromAsset("../aws-ecommerce-lambda-functions/", AssetOptions.builder()
	                        .bundling(builderOptions
	                                .command(functionPackagingInstructions)
	                                .build())
	                        .build()))
	                .handler("com.br.aws.ecommerce.product.ProductsAdminFunction")
	                .memorySize(512)
	                .timeout(Duration.seconds(10))
	                .logRetention(RetentionDays.ONE_WEEK)
	                .build()));
		
	}
	
	 

	 

}
