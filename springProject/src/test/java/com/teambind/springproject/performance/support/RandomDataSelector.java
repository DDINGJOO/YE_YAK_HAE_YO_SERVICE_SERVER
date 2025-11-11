package com.teambind.springproject.performance.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 랜덤 데이터 선택 유틸리티.
 * <p>
 * 성능 테스트에서 랜덤한 위치의 데이터를 선택하기 위한 유틸리티입니다.
 */
public final class RandomDataSelector {

  private final Random random;

  public RandomDataSelector() {
    this.random = new Random();
  }

  public RandomDataSelector(final long seed) {
    this.random = new Random(seed);
  }

  /**
   * 범위 내에서 랜덤한 Long 값을 생성합니다.
   *
   * @param minInclusive 최소 값 (포함)
   * @param maxExclusive 최대 값 (미포함)
   * @return 랜덤 Long 값
   */
  public long randomLong(final long minInclusive, final long maxExclusive) {
    if (minInclusive >= maxExclusive) {
      throw new IllegalArgumentException(
          "minInclusive must be less than maxExclusive");
    }
    final long range = maxExclusive - minInclusive;
    return minInclusive + (long) (random.nextDouble() * range);
  }

  /**
   * 여러 개의 랜덤 Long 값을 생성합니다.
   *
   * @param count 생성할 개수
   * @param minInclusive 최소 값 (포함)
   * @param maxExclusive 최대 값 (미포함)
   * @return 랜덤 Long 값 리스트
   */
  public List<Long> randomLongs(
      final int count,
      final long minInclusive,
      final long maxExclusive) {
    final List<Long> results = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      results.add(randomLong(minInclusive, maxExclusive));
    }
    return results;
  }

  /**
   * 리스트에서 랜덤한 요소를 선택합니다.
   *
   * @param list 선택할 리스트
   * @param <T> 요소 타입
   * @return 랜덤 요소
   */
  public <T> T randomElement(final List<T> list) {
    if (list.isEmpty()) {
      throw new IllegalArgumentException("List must not be empty");
    }
    final int index = random.nextInt(list.size());
    return list.get(index);
  }
}