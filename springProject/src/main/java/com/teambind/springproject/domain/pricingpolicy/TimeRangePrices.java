package com.teambind.springproject.domain.pricingpolicy;

import com.teambind.springproject.domain.shared.DayOfWeek;
import com.teambind.springproject.domain.shared.Money;
import com.teambind.springproject.domain.shared.TimeSlot;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 시간대별 가격 목록을 관리하는 Value Object.
 * 시간대 중복을 검증하고 특정 슬롯에 대한 가격을 찾는 기능을 제공합니다.
 */
public class TimeRangePrices {

  private final List<TimeRangePrice> prices;

  private TimeRangePrices(final List<TimeRangePrice> prices) {
    validatePrices(prices);
    this.prices = new ArrayList<>(prices);
  }

  public static TimeRangePrices of(final List<TimeRangePrice> prices) {
    return new TimeRangePrices(prices);
  }

  public static TimeRangePrices empty() {
    return new TimeRangePrices(Collections.emptyList());
  }

  private void validatePrices(final List<TimeRangePrice> prices) {
    if (prices == null) {
      throw new IllegalArgumentException("Prices cannot be null");
    }

    // 시간대 중복 검증
    for (int i = 0; i < prices.size(); i++) {
      for (int j = i + 1; j < prices.size(); j++) {
        if (prices.get(i).overlaps(prices.get(j))) {
          throw new IllegalArgumentException(
              "Time range prices cannot overlap: "
                  + prices.get(i) + " and " + prices.get(j));
        }
      }
    }
  }

  /**
   * 특정 요일과 시간에 해당하는 가격을 찾습니다.
   *
   * @param dayOfWeek 요일
   * @param time 시간
   * @return 해당하는 가격 (없으면 Optional.empty())
   */
  public Optional<Money> findPriceForSlot(final DayOfWeek dayOfWeek, final LocalTime time) {
    if (dayOfWeek == null) {
      throw new IllegalArgumentException("Day of week cannot be null");
    }
    if (time == null) {
      throw new IllegalArgumentException("Time cannot be null");
    }

    return prices.stream()
        .filter(price -> price.dayOfWeek() == dayOfWeek)
        .filter(price -> price.timeRange().contains(time))
        .map(TimeRangePrice::pricePerSlot)
        .findFirst();
  }

  /**
   * 모든 시간대 가격의 총합을 계산합니다.
   *
   * @param timeSlot 시간 단위
   * @return 총 가격
   */
  public Money calculateTotal(final TimeSlot timeSlot) {
    if (timeSlot == null) {
      throw new IllegalArgumentException("TimeSlot cannot be null");
    }

    Money total = Money.ZERO;
    for (final TimeRangePrice price : prices) {
      final int slots = price.timeRange().calculateSlots(timeSlot);
      final Money slotTotal = price.pricePerSlot().multiply(slots);
      total = total.add(slotTotal);
    }
    return total;
  }

  public List<TimeRangePrice> getPrices() {
    return Collections.unmodifiableList(prices);
  }

  public boolean isEmpty() {
    return prices.isEmpty();
  }

  public int size() {
    return prices.size();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final TimeRangePrices that = (TimeRangePrices) o;
    return Objects.equals(prices, that.prices);
  }

  @Override
  public int hashCode() {
    return Objects.hash(prices);
  }

  @Override
  public String toString() {
    return "TimeRangePrices{" + prices.size() + " entries}";
  }
}
