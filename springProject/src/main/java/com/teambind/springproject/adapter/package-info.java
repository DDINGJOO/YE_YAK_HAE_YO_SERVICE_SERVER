/**
 * Adapter Layer (어댑터 레이어)
 * <p>
 * 외부 세계와 애플리케이션을 연결하는 레이어입니다.
 * Hexagonal Architecture의 Adapter 역할을 수행합니다.
 * <p>
 * 구성 요소:
 * - Inbound Adapter (Primary): 외부 → 애플리케이션 (REST Controller 등)
 * - Outbound Adapter (Secondary): 애플리케이션 → 외부 (JPA Repository, Kafka Producer 등)
 * <p>
 * 책임:
 * - 외부 요청/응답 형식 변환 (JSON, XML 등)
 * - Port 구현 (Inbound/Outbound)
 * - 기술 세부사항 처리 (HTTP, Database, Messaging 등)
 * <p>
 * 설계 원칙:
 * - 도메인 레이어에 의존하지 않음 (Application Port에만 의존)
 * - 변경 가능성이 높은 기술 스택 격리
 * - Plug & Play 가능한 구조
 */
package com.teambind.springproject.adapter;
