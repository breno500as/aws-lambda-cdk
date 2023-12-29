package com.myorg;

import software.amazon.awscdk.services.lambda.Function;

public class EcommerceCommons {

	private Function productsFetchFunction;

	private Function productsAdminFunction;

	private Function eventsFunction;

	public Function getProductsFetchFunction() {
		return productsFetchFunction;
	}

	public void setProductsFetchFunction(Function productsFetchFunction) {
		this.productsFetchFunction = productsFetchFunction;
	}

	public Function getProductsAdminFunction() {
		return productsAdminFunction;
	}

	public void setProductsAdminFunction(Function productsAdminFunction) {
		this.productsAdminFunction = productsAdminFunction;
	}

	public Function getEventsFunction() {
		return eventsFunction;
	}

	public void setEventsFunction(Function eventsFunction) {
		this.eventsFunction = eventsFunction;
	}

}
