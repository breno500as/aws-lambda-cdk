package com.myorg;

import java.util.Arrays;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.LayerVersion;
import software.amazon.awscdk.services.lambda.LayerVersionProps;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.amazon.awscdk.services.ssm.StringParameterProps;
import software.constructs.Construct;

public class LambdaLayersStack extends Stack implements DockerBuildStack {

	 
	private static final String PROJECT_LAMBDA_LAYERS_NAME = "aws-ecommerce-lambda-layers";
	
	public static final String PRODUCTS_LAYER_VERSION_ARN = "ProductsLayerVersionArn";
	
	public static final String EVENTS_LAYER_VERSION_ARN = "EventsLayerVersionArn";

	public LambdaLayersStack(final Construct scope, final String id, StackProps stackProps) {
		super(scope, id, stackProps);
		this.createLayerProduct();
		this.createLayerEvent();
	}

	private void createLayerEvent() {
		final LayerVersion eventLayers  = new LayerVersion(this, "EventsLayer", LayerVersionProps.builder()
                .code(Code.fromAsset("../" + PROJECT_LAMBDA_LAYERS_NAME + "/",
                           AssetOptions.builder()
                                       .bundling(getBundlingOptions(PROJECT_LAMBDA_LAYERS_NAME))
                                       .build()))
                .layerVersionName("EventsLayer")
                .removalPolicy(RemovalPolicy.RETAIN)
                .compatibleRuntimes(Arrays.asList(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME))
                .build());

        // Cria um parâmetro no system manager parameter store para que a layer possa ser referenciada em uma lambda function
		new StringParameter(this, EVENTS_LAYER_VERSION_ARN, StringParameterProps.builder()
				                                               .parameterName(EVENTS_LAYER_VERSION_ARN)
				                                               .stringValue(eventLayers.getLayerVersionArn()).build());
		
	}

	private void createLayerProduct() {
		
		final LayerVersion productLayers  = new LayerVersion(this, "ProductsLayer", LayerVersionProps.builder()
                .code(Code.fromAsset("../" + PROJECT_LAMBDA_LAYERS_NAME + "/",
                           AssetOptions.builder()
                                       .bundling(getBundlingOptions(PROJECT_LAMBDA_LAYERS_NAME))
                                       .build()))
                .layerVersionName("ProductsLayer")
                .removalPolicy(RemovalPolicy.RETAIN)
                .compatibleRuntimes(Arrays.asList(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME))
                .build());

        // Cria um parâmetro no system manager parameter store para que a layer possa ser referenciada em uma lambda function
		new StringParameter(this, PRODUCTS_LAYER_VERSION_ARN, StringParameterProps.builder()
				                                               .parameterName(PRODUCTS_LAYER_VERSION_ARN)
				                                               .stringValue(productLayers.getLayerVersionArn()).build());
		
	}
	

	 

}
