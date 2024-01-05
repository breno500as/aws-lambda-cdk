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

public class LambdaLayersStack extends Stack implements BaseDockerBuild {

	 
	private static final String PROJECT_LAMBDA_LAYERS_NAME = "aws-ecommerce-lambda-layers";
	
	public static final String ECOMMERCE_LAYER_VERSION_ARN = "EcommerceLayerVersionArn";

	public LambdaLayersStack(final Construct scope, final String id, StackProps stackProps) {
		super(scope, id, stackProps);
		
		// Todas as funções (que poderiam estar distribuidas em vários arquivos jar diferentes) 
		// apontam para a mesma layer implantanda em um único  arquivo jar que
		// contém classes de modelo comuns e repositórios para acesso ao dynamodb.
		// porém poderia se criar várias layers com nomes distintos (ou seja uma layer apenas com um modelo e um repositório específico
		// diminuindo o tamanho) apontando para arquivos jar diferentes
		// e apontar cada layer para cada arquivo jar de funções específicos

		this.createLayerEcommerce();

	}

 
	
	private void createLayerEcommerce() {
		
		final LayerVersion ecommerceLayer  = new LayerVersion(this, "EcommerceLayer", LayerVersionProps.builder()
                .code(Code.fromAsset("../" + PROJECT_LAMBDA_LAYERS_NAME + "/",
                           AssetOptions.builder()
                                       .bundling(getBundlingOptions(PROJECT_LAMBDA_LAYERS_NAME))
                                       .build()))
                .layerVersionName("EcommerceLayer")
                .removalPolicy(RemovalPolicy.DESTROY)
                .compatibleRuntimes(Arrays.asList(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME))
                .build());

        // Cria um parâmetro no system manager parameter store para que a layer possa ser referenciada em uma lambda function
		new StringParameter(this, ECOMMERCE_LAYER_VERSION_ARN, StringParameterProps.builder()
				                                               .parameterName(ECOMMERCE_LAYER_VERSION_ARN)
				                                               .stringValue(ecommerceLayer.getLayerVersionArn()).build());
		
	}
	

	 

}
