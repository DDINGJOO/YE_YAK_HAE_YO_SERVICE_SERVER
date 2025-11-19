package com.teambind.springproject.adapter.out.messaging.kafka;


import com.teambind.springproject.adapter.out.messaging.kafka.event.Event;
import com.teambind.springproject.adapter.out.messaging.kafka.event.dto.EventDtoFactory;
import com.teambind.springproject.application.port.out.publisher.EventPublisher;
import com.teambind.springproject.common.util.json.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.TimeUnit;


@Component
@RequiredArgsConstructor
public class KafkaEventPublisher  implements EventPublisher {
	private static final Logger logger = org.slf4j.LoggerFactory.getLogger(KafkaEventPublisher.class);
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final JsonUtil jsonUtil;
	@Override
	public void publish(final Event event) {
		final String json;
		try {
			// Event -> DTO 변환 (Long ID를 String으로 직렬화하기 위함)
			final Object eventDto = EventDtoFactory.createDto(event);
			json = jsonUtil.toJson(eventDto);
		} catch (final Exception e) {
			logger.error("Failed to serialize event: {}", event, e);
			throw new IllegalArgumentException("Failed to serialize event", e);
		}

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(
					new TransactionSynchronization() {
						@Override
						public void afterCommit() {
							publishToKafka(event.getTopic(), json, event);
						}
					}
			);
			logger.debug("Event publishing scheduled after transaction commit: {}", event.getEventTypeName());
		} else {
			publishToKafka(event.getTopic(), json, event);
		}
	}
	
	private void publishToKafka(final String topic, final String json, final Event originalEvent) {
		try{
			kafkaTemplate.send(topic, json).get(5, TimeUnit.SECONDS);
			logger.info("Published event {} to kafka", originalEvent.getEventTypeName());
		}catch (Exception e )
		{
			logger.error("[CRITICAL]Failed to publish event {} to kafka", originalEvent.getEventTypeName(), e);
			// TODO: 알람 발송 (Slack, PagerDuty 등)
			// alertService.sendCritical("Kafka event publish failed", topic, originalEvent, e);

		}
	}
}
