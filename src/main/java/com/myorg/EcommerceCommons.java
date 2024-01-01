package com.myorg;

import software.amazon.awscdk.services.lambda.Function;

public class EcommerceCommons {

	private Function productsFetchFunction;

	private Function productsAdminFunction;

	private Function eventsFunction;

	private Function ordersFunction;

	private Function ordersEventFunction;

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

	public Function getOrdersFunction() {
		return ordersFunction;
	}

	public void setOrdersFunction(Function ordersFunction) {
		this.ordersFunction = ordersFunction;
	}

	public Function getOrdersEventFunction() {
		return ordersEventFunction;
	}

	public void setOrdersEventFunction(Function ordersEventFunction) {
		this.ordersEventFunction = ordersEventFunction;
	}

}
