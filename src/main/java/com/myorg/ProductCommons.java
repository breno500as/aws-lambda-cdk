package com.myorg;

import software.amazon.awscdk.services.lambda.Function;

public class ProductCommons {

	private Function productsFetchFunction;

	private Function productsAdminFunction;

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

}
