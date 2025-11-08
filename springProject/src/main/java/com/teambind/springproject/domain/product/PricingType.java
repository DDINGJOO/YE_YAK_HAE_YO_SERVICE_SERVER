package com.teambind.springproject.domain.product;

/**
 * 상품의 가격 책정 방식을 나타내는 Enum.
 */
public enum PricingType {

  /**
   * 초기 대여료 + 추가 요금 방식.
   * 예: 초기 2시간 10,000원 + 추가 시간당 5,000원
   */
  INITIAL_PLUS_ADDITIONAL,

  /**
   * 1회 대여료 고정 방식.
   * 예: 1회 대여 시 15,000원 (시간 무관)
   */
  ONE_TIME,

  /**
   * 단순 재고 관리 방식.
   * 시간과 무관하게 수량만 관리하는 방식
   */
  SIMPLE_STOCK
}
