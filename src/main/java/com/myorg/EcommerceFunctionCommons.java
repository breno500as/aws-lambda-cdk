package com.myorg;

import software.amazon.awscdk.services.lambda.Function;

public class EcommerceFunctionCommons {

	private Function productsFetchFunction;

	private Function productsAdminFunction;

	private Function productEventFunction;

	private Function ordersFunction;

	private Function ordersEventFunction;

	private Function ordersEventFetchFunction;

	private Function ordersPaymentFunction;

	private Function ordersEmailFunction;

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

	public Function getProductEventFunction() {
		return productEventFunction;
	}

	public void setProductEventFunction(Function productEventFunction) {
		this.productEventFunction = productEventFunction;
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

	public Function getOrdersPaymentFunction() {
		return ordersPaymentFunction;
	}

	public void setOrdersPaymentFunction(Function ordersPaymentFunction) {
		this.ordersPaymentFunction = ordersPaymentFunction;
	}

	public Function getOrdersEmailFunction() {
		return ordersEmailFunction;
	}

	public void setOrdersEmailFunction(Function ordersEmailFunction) {
		this.ordersEmailFunction = ordersEmailFunction;
	}

	public Function getOrdersEventFetchFunction() {
		return ordersEventFetchFunction;
	}

	public void setOrdersEventFetchFunction(Function ordersEventFetchFunction) {
		this.ordersEventFetchFunction = ordersEventFetchFunction;
	}

}
