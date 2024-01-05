package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.cloudwatch.Alarm;
import software.amazon.awscdk.services.cloudwatch.ComparisonOperator;
import software.amazon.awscdk.services.cloudwatch.CreateAlarmOptions;
import software.amazon.awscdk.services.cloudwatch.Metric;
import software.amazon.awscdk.services.cloudwatch.MetricOptions;
import software.amazon.awscdk.services.cloudwatch.TreatMissingData;
import software.amazon.awscdk.services.cloudwatch.Unit;
import software.amazon.awscdk.services.cloudwatch.actions.SnsAction;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.logs.FilterPattern;
import software.amazon.awscdk.services.logs.MetricFilter;
import software.amazon.awscdk.services.logs.MetricFilterOptions;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.subscriptions.EmailSubscription;
import software.constructs.Construct;

public class CloudWatchAlarmsStack extends Stack {

	public CloudWatchAlarmsStack(final Construct scope, final String id, EcommerceCommons ecommerceCommons,
			StackProps stackProps) {

		super(scope, id, stackProps);

		this.createLambdaOrdersAlarm(ecommerceCommons);
		this.createSQSAuditTimeoutAlarm(ecommerceCommons);
		this.createDynamoDbWriteUnitsAlarm(ecommerceCommons);

	}
	
	private void createDynamoDbWriteUnitsAlarm(EcommerceCommons ecommerceCommons) {
		
		// Métrica para unidades de leitura em gargalo com uma mética customizada pré estabelecida pela aws WriteThottleEvents
		final Metric writeThottleEventsMetric =  ecommerceCommons.getOrderTable()
				.metric("WriteThottleEvents", MetricOptions
						.builder()
						.period(Duration.minutes(2))
						.statistic("SampleCount")
						.unit(Unit.COUNT)
						.build());
		
		writeThottleEventsMetric.createAlarm(this, "WriteThottleOrderEvents", CreateAlarmOptions.builder()
				.alarmName("WriteThottleOrderEvents")
				.alarmDescription("Thottle write events in table Order ")
				.evaluationPeriods(1) // Avalie um único período no caso de 2 minutos
				.threshold(10) // Limite - 10 requisições de escrita estranguladas
				.comparisonOperator(ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD)
				.treatMissingData(TreatMissingData.NOT_BREACHING) // Deixe o alarme no status OK caso não possua dados
				.build());
		
	}

	private void createSQSAuditTimeoutAlarm(EcommerceCommons ecommerceCommons) {
		
		 
		final Metric ageOfMessagesMetric = ecommerceCommons
				.getInvoiceTimeoutQueue()
				.metricApproximateAgeOfOldestMessage(MetricOptions
						.builder()
						.period(Duration.minutes(2))
						.statistic("Maximum")
						.unit(Unit.SECONDS)
						.build());
		
		
		ageOfMessagesMetric.createAlarm(this, "AgeOfMessagesQueueInvoiceImportTimeoutAlarm", CreateAlarmOptions.builder()
				.alarmName("AgeOfMessagesQueueInvoiceImportTimeoutAlarm")
				.alarmDescription("Max age of messages handle by Invoice Import Timeout Queue ")
				.evaluationPeriods(1) // Avalie um único período no caso de 2 minutos
				.threshold(60) // Limite - Nesse caso de tempo para a mensagem ser tratada
				.comparisonOperator(ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD)
				.build());
		
		
		final Metric numberOfMessagesMetric = ecommerceCommons
				.getInvoiceTimeoutQueue()
				.metricApproximateNumberOfMessagesVisible(MetricOptions
						.builder()
						.period(Duration.minutes(2))
						.statistic("Sum")
						.build());
		
		
		numberOfMessagesMetric.createAlarm(this, "NumberOfMessagesInvoiceImportTimeoutQueueAlarm", CreateAlarmOptions.builder()
				.alarmName("NumberOfMessagesInvoiceImportTimeoutQueueAlarm")
				.alarmDescription("Five messages queues accumulated in 2 minutes")
				.evaluationPeriods(1) // Avalie um único período no caso de 2 minutos
				.threshold(5) // Limite - Nesse caso de número de ocorrências dentro do período para gerar o alarme
				.comparisonOperator(ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD)
				.build());
		 

	}

	private void createLambdaOrdersAlarm(EcommerceCommons ecommerceCommons) {

		final MetricFilter productNotFoudMetricFilter = ecommerceCommons.getOrdersFunction().getLogGroup()
				.addMetricFilter("ProductNotFoundMetric", MetricFilterOptions
						.builder()
						.metricName("OrderNonValidProduct")
						.metricNamespace("ProductNotFound")
						.filterPattern(FilterPattern.literal("Product not found"))
						.build());
		
		final Alarm productNotFoudAlarm = productNotFoudMetricFilter.metric()
				.with(MetricOptions.builder().statistic("Sum").period(Duration.minutes(2)).build())
				.createAlarm(productNotFoudMetricFilter, "ProductNotFoudAlarm", CreateAlarmOptions.builder()
						.alarmName("ProductNotFoudAlarm")
						.alarmDescription("Five products not found in 2 minutes")
						.evaluationPeriods(1) // Avalie um único período no caso de 2 minutos
						.threshold(5) // Limite de número de ocorrências dentro do período para gerar o alarme
						.actionsEnabled(true)
						.comparisonOperator(ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD)
						.build());
		
		
	       final SnsTopic orderAlarmsSnsTopic = SnsTopic.Builder
						.create(Topic.Builder.create(this, "OrderAlarmsTopic").topicName("order-alarms-topic").build()).build();
	       
	       
	       orderAlarmsSnsTopic.getTopic().addSubscription(new EmailSubscription("breno500as@gmail.com"));
	       
	       productNotFoudAlarm.addAlarmAction(new SnsAction(orderAlarmsSnsTopic.getTopic()));

	}

}
