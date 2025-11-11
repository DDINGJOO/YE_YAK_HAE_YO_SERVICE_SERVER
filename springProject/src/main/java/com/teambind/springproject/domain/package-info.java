/**
 * Domain Layer (도메인 레이어)
 * <p>
 * 비즈니스 로직의 핵심을 담당하는 레이어입니다.
 * 외부 기술(Framework, Database 등)에 의존하지 않으며, 순수한 Java 코드로 구성됩니다.
 * <p>
 * 구성 요소:
 * - Aggregate Root: 트랜잭션 일관성 경계를 정의하는 엔티티
 * - Entity: 고유 식별자를 가진 도메인 객체
 * - Value Object: 불변 객체로, 속성의 조합으로 식별
 * - Domain Service: 여러 Aggregate에 걸친 비즈니스 로직
 * - Repository Interface: 영속성 추상화 (Port)
 * <p>
 * 설계 원칙:
 * - 도메인 규칙(Invariants)을 강제합니다
 * - 불변성(Immutability)을 최대한 활용합니다
 * - 외부 레이어에 의존하지 않습니다 (Dependency Inversion)
 */
package com.teambind.springproject.domain;
