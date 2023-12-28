package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;

public class LambdaFunctionEventsStack extends Stack {
	
	
	public LambdaFunctionEventsStack(final Construct scope, final String id, ProductCommons productCommonsStack, final StackProps props) {
		
		   super(scope, id, props);
		   
	}

}
