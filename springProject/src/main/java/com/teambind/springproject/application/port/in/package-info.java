/**
 * Inbound Ports (Primary Ports)
 * <p>
 * 외부에서 애플리케이션으로 들어오는 요청을 정의하는 인터페이스입니다.
 * Use Case를 표현합니다.
 * <p>
 * 설계 원칙:
 * - 하나의 인터페이스는 하나의 Use Case를 표현
 * - 명령(Command)과 조회(Query)를 분리 (CQRS 패턴)
 * - 비즈니스 용어를 사용한 명확한 메서드명
 */
package com.teambind.springproject.application.port.in;
