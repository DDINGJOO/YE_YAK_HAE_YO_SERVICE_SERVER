/**
 * Application Layer (애플리케이션 레이어)
 *
 * Use Case를 구현하고 도메인 객체들을 조율하는 레이어입니다.
 * 트랜잭션 경계를 정의하고, 도메인 로직을 호출합니다.
 *
 * 구성 요소:
 * - Inbound Port: Use Case 인터페이스 (Primary Port)
 * - Outbound Port: Infrastructure 추상화 인터페이스 (Secondary Port)
 * - Application Service: Use Case 구현체
 * - DTO: 외부 레이어와 데이터 교환용 객체
 *
 * 책임:
 * - Use Case 흐름 제어
 * - 트랜잭션 관리 (@Transactional)
 * - 도메인 객체 간 조율 (Orchestration)
 * - DTO ↔ Domain 변환
 *
 * 설계 원칙:
 * - 얇은 레이어로 유지 (Thin Layer)
 * - 비즈니스 로직은 도메인 레이어에 위임
 * - Infrastructure 세부사항을 알지 못함 (Port를 통해 추상화)
 */
package com.teambind.springproject.application;
