package com.teambind.springproject.adapter.out.messaging.kafka.event.dto;

import com.teambind.springproject.adapter.out.messaging.kafka.event.Event;
import com.teambind.springproject.adapter.out.messaging.kafka.event.ReservationCancelledEvent;
import com.teambind.springproject.adapter.out.messaging.kafka.event.ReservationPendingPaymentEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Event to DTO 변환을 담당하는 Factory 클래스.
 *
 * Factory Pattern을 사용하여 변환 로직을 중앙화하고,
 * Open-Closed Principle을 준수합니다 (새 이벤트 추가 시 확장만 필요).
 *
 * 각 이벤트 타입별로 적절한 DTO 변환기를 등록하고,
 * 런타임에 이벤트 타입에 따라 올바른 변환기를 선택합니다.
 */
public final class EventDtoFactory {

	private static final Map<Class<? extends Event>, Function<Event, Object>> converters = new HashMap<>();

	static {
		registerConverter(ReservationPendingPaymentEvent.class,
				event -> ReservationPendingPaymentEventDto.from((ReservationPendingPaymentEvent) event));

		registerConverter(ReservationCancelledEvent.class,
				event -> ReservationCancelledEventDto.from((ReservationCancelledEvent) event));
	}

	private EventDtoFactory() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	/**
	 * 이벤트 타입별 변환기를 등록합니다.
	 *
	 * @param eventClass 이벤트 클래스
	 * @param converter 변환 함수
	 * @param <T> 이벤트 타입
	 */
	private static <T extends Event> void registerConverter(
			final Class<T> eventClass,
			final Function<Event, Object> converter) {
		converters.put(eventClass, converter);
	}

	/**
	 * 도메인 이벤트를 Kafka 발행용 DTO로 변환합니다.
	 *
	 * @param event 변환할 도메인 이벤트
	 * @return Kafka 발행용 DTO
	 * @throws IllegalArgumentException 등록되지 않은 이벤트 타입인 경우
	 */
	public static Object createDto(final Event event) {
		final Function<Event, Object> converter = converters.get(event.getClass());

		if (converter == null) {
			throw new IllegalArgumentException(
					"No DTO converter registered for event type: " + event.getClass().getName()
							+ ". Please register a converter in EventDtoFactory."
			);
		}

		return converter.apply(event);
	}

	/**
	 * 특정 이벤트 타입에 대한 변환기가 등록되어 있는지 확인합니다.
	 *
	 * @param eventClass 확인할 이벤트 클래스
	 * @return 변환기 등록 여부
	 */
	public static boolean hasConverter(final Class<? extends Event> eventClass) {
		return converters.containsKey(eventClass);
	}
}