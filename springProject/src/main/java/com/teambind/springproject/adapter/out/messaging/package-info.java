/**
 * Messaging Outbound Adapter (Kafka Event Handlers)
 * <p>
 * Kafka를 통해 도메인 이벤트를 발행하고, 외부 이벤트를 수신하는 어댑터입니다.
 * <p>
 * 책임:
 * - 도메인 이벤트 → Kafka 메시지 변환
 * - Kafka 메시지 발행 (Producer)
 * - 외부 이벤트 수신 (Consumer)
 * - 이벤트 직렬화/역직렬화
 * <p>
 * 설계 원칙:
 * - 멱등성 보장 (중복 이벤트 처리)
 * - At-least-once 또는 Exactly-once 보장
 * - 이벤트 스키마 버전 관리
 */
package com.teambind.springproject.adapter.out.messaging;
