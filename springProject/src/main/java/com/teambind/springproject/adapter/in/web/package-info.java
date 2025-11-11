/**
 * Web Inbound Adapter (REST Controllers)
 * <p>
 * HTTP 요청을 받아 Use Case를 호출하고, 응답을 반환하는 REST API Controller입니다.
 * <p>
 * 책임:
 * - HTTP 요청 → Command/Query 변환
 * - Use Case 호출
 * - 결과 → HTTP 응답 변환
 * - HTTP 상태 코드 결정
 * - 입력 Validation (Bean Validation)
 * <p>
 * 설계 원칙:
 * - RESTful API 설계 원칙 준수
 * - 얇은 레이어로 유지 (비즈니스 로직 포함 금지)
 * - 명확한 예외 처리 및 응답
 */
package com.teambind.springproject.adapter.in.web;
