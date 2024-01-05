package com.myorg;

import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.sqs.Queue;

public class EcommerceCommons {

	private Function productsFetchFunction;

	private Function productsAdminFunction;

	private Function productEventFunction;

	private Function ordersFunction;

	private Function ordersEventFunction;

	private Function ordersEventFetchFunction;

	private Function ordersPaymentFunction;

	private Function ordersEmailFunction;

	private Function invoiceEventFunction;

	private Function invoiceCancelImportFunction;

	private Function invoiceImportFunction;

	private Function invoiceGetUrlFunction;

	private Function invoiceDefaultFunction;

	private Function invoiceDisconnectionFunction;

	private Function invoiceConnectionFunction;
	
	private Queue invoiceTimeoutQueue;
	
	private Table orderTable;

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

	public Function getInvoiceEventFunction() {
		return invoiceEventFunction;
	}

	public void setInvoiceEventFunction(Function invoiceEventFunction) {
		this.invoiceEventFunction = invoiceEventFunction;
	}

	public Function getInvoiceCancelImportFunction() {
		return invoiceCancelImportFunction;
	}

	public void setInvoiceCancelImportFunction(Function invoiceCancelImportFunction) {
		this.invoiceCancelImportFunction = invoiceCancelImportFunction;
	}

	public Function getInvoiceImportFunction() {
		return invoiceImportFunction;
	}

	public void setInvoiceImportFunction(Function invoiceImportFunction) {
		this.invoiceImportFunction = invoiceImportFunction;
	}

	public Function getInvoiceGetUrlFunction() {
		return invoiceGetUrlFunction;
	}

	public void setInvoiceGetUrlFunction(Function invoiceGetUrlFunction) {
		this.invoiceGetUrlFunction = invoiceGetUrlFunction;
	}

	public Function getInvoiceDefaultFunction() {
		return invoiceDefaultFunction;
	}

	public void setInvoiceDefaultFunction(Function invoiceDefaultFunction) {
		this.invoiceDefaultFunction = invoiceDefaultFunction;
	}

	public Function getInvoiceDisconnectionFunction() {
		return invoiceDisconnectionFunction;
	}

	public void setInvoiceDisconnectionFunction(Function invoiceDisconnectionFunction) {
		this.invoiceDisconnectionFunction = invoiceDisconnectionFunction;
	}

	public Function getInvoiceConnectionFunction() {
		return invoiceConnectionFunction;
	}

	public void setInvoiceConnectionFunction(Function invoiceConnectionFunction) {
		this.invoiceConnectionFunction = invoiceConnectionFunction;
	}
	
	public Queue getInvoiceTimeoutQueue() {
		return invoiceTimeoutQueue;
	}
	
	public void setInvoiceTimeoutQueue(Queue invoiceTimeoutQueue) {
		this.invoiceTimeoutQueue = invoiceTimeoutQueue;
	}
	
	public Table getOrderTable() {
		return orderTable;
	}
	
	public void setOrderTable(Table orderTable) {
		this.orderTable = orderTable;
	}

}
