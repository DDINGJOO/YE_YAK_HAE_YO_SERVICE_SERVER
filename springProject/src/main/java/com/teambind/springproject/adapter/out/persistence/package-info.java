/**
 * Persistence Outbound Adapter (JPA Repositories)
 *
 * 도메인 객체를 데이터베이스에 저장하고 조회하는 영속성 어댑터입니다.
 *
 * 구성 요소:
 * - JPA Entity: 데이터베이스 테이블과 매핑되는 클래스
 * - JPA Repository: Spring Data JPA 인터페이스
 * - Repository Adapter: Domain Repository Port 구현체
 * - Mapper: JPA Entity ↔ Domain Model 변환
 *
 * 책임:
 * - Domain Model → JPA Entity 변환
 * - JPA Entity → Domain Model 변환
 * - Domain Repository Port 구현
 * - 데이터베이스 접근 로직
 *
 * 설계 원칙:
 * - JPA Entity는 도메인 모델과 분리 (별도 클래스)
 * - 변환 로직을 명확히 구현 (Mapper 사용)
 * - 도메인 규칙은 도메인 레이어에 위임
 */
package com.teambind.springproject.adapter.out.persistence;
