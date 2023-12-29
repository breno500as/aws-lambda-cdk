package com.myorg;

import static java.util.Collections.singletonList;
import static software.amazon.awscdk.BundlingOutput.ARCHIVED;

import java.util.Arrays;

import software.amazon.awscdk.BundlingOptions;
import software.amazon.awscdk.DockerVolume;

public interface DockerBuildStack {

	public default BundlingOptions getBundlingOptions(String projectName) {
		
		return BundlingOptions.builder()
				.command(Arrays.asList("/bin/sh", 
						               "-c",
						               "mvn clean install " + "&& cp /asset-input/target/" + projectName + ".jar /asset-output/"))
				.image(AwsLambdaCdkApp.PROJECT_JAVA_RUNTIME.getBundlingImage())
				// Mount local .m2 repo to avoid download all the dependencies again inside the
				// container
				.volumes(singletonList(DockerVolume.builder()
						                           .hostPath(System.getProperty("user.home") + "/.m2/")
								                   .containerPath("/root/.m2/")
								                   .build()))
				.user("root")
				.outputType(ARCHIVED)
				.build();
	}

}
