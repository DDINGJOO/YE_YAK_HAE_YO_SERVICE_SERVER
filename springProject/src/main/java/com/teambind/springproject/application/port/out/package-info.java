/**
 * Outbound Ports (Secondary Ports)
 * <p>
 * 애플리케이션에서 외부 Infrastructure로 나가는 요청을 정의하는 인터페이스입니다.
 * Repository, Event Publisher 등을 추상화합니다.
 * <p>
 * 포함 요소:
 * - Repository Interface (도메인 레이어에 정의된 것을 확장할 수도 있음)
 * - Event Publisher Interface
 * - External API Client Interface
 * <p>
 * 설계 원칙:
 * - Infrastructure 세부사항을 숨김
 * - 도메인 중심 인터페이스 (기술 용어 배제)
 * - 테스트 가능성 향상 (Mock 객체로 대체 가능)
 */
package com.teambind.springproject.application.port.out;
