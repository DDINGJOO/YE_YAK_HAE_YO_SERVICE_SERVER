package com.teambind.springproject.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Money Value Object 테스트")
class MoneyTest {

  @Nested
  @DisplayName("생성 테스트")
  class CreationTests {

    @Test
    @DisplayName("BigDecimal로 Money 생성 성공")
    void createMoneyWithBigDecimal() {
      // given
      final BigDecimal amount = new BigDecimal("10000.50");

      // when
      final Money money = Money.of(amount);

      // then
      assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("10000.50"));
    }

    @Test
    @DisplayName("long으로 Money 생성 성공")
    void createMoneyWithLong() {
      // given
      final long amount = 10000L;

      // when
      final Money money = Money.of(amount);

      // then
      assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    @DisplayName("double로 Money 생성 성공")
    void createMoneyWithDouble() {
      // given
      final double amount = 10000.50;

      // when
      final Money money = Money.of(amount);

      // then
      assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("10000.50"));
    }

    @Test
    @DisplayName("ZERO 상수 확인")
    void zeroConstant() {
      // when & then
      assertThat(Money.ZERO.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("소수점 2자리로 반올림 확인")
    void roundingToTwoDecimalPlaces() {
      // given
      final BigDecimal amount = new BigDecimal("10000.555");

      // when
      final Money money = Money.of(amount);

      // then
      assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("10000.56"));
    }
  }

  @Nested
  @DisplayName("유효성 검증 테스트")
  class ValidationTests {

    @Test
    @DisplayName("null 금액으로 생성 시 예외 발생")
    void throwExceptionWhenAmountIsNull() {
      // when & then
      assertThatThrownBy(() -> Money.of((BigDecimal) null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Amount cannot be null");
    }

    @Test
    @DisplayName("음수 금액으로 생성 시 예외 발생")
    void throwExceptionWhenAmountIsNegative() {
      // given
      final BigDecimal negativeAmount = new BigDecimal("-100");

      // when & then
      assertThatThrownBy(() -> Money.of(negativeAmount))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Amount cannot be negative: -100");
    }

    @Test
    @DisplayName("0은 유효한 금액")
    void zeroIsValidAmount() {
      // given
      final BigDecimal zero = BigDecimal.ZERO;

      // when
      final Money money = Money.of(zero);

      // then
      assertThat(money.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
  }

  @Nested
  @DisplayName("산술 연산 테스트")
  class ArithmeticTests {

    @Test
    @DisplayName("두 Money 더하기")
    void addTwoMoneyObjects() {
      // given
      final Money money1 = Money.of(new BigDecimal("10000"));
      final Money money2 = Money.of(new BigDecimal("5000"));

      // when
      final Money result = money1.add(money2);

      // then
      assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("15000.00"));
    }

    @Test
    @DisplayName("더하기 시 null 파라미터 예외 발생")
    void throwExceptionWhenAddingNull() {
      // given
      final Money money = Money.of(new BigDecimal("10000"));

      // when & then
      assertThatThrownBy(() -> money.add(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Other money cannot be null");
    }

    @Test
    @DisplayName("두 Money 빼기")
    void subtractTwoMoneyObjects() {
      // given
      final Money money1 = Money.of(new BigDecimal("10000"));
      final Money money2 = Money.of(new BigDecimal("3000"));

      // when
      final Money result = money1.subtract(money2);

      // then
      assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("7000.00"));
    }

    @Test
    @DisplayName("빼기 결과가 음수일 경우 예외 발생")
    void throwExceptionWhenSubtractionResultIsNegative() {
      // given
      final Money money1 = Money.of(new BigDecimal("5000"));
      final Money money2 = Money.of(new BigDecimal("10000"));

      // when & then
      assertThatThrownBy(() -> money1.subtract(money2))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Result cannot be negative");
    }

    @Test
    @DisplayName("빼기 시 null 파라미터 예외 발생")
    void throwExceptionWhenSubtractingNull() {
      // given
      final Money money = Money.of(new BigDecimal("10000"));

      // when & then
      assertThatThrownBy(() -> money.subtract(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Other money cannot be null");
    }

    @Test
    @DisplayName("정수로 곱하기")
    void multiplyByInteger() {
      // given
      final Money money = Money.of(new BigDecimal("1000"));

      // when
      final Money result = money.multiply(5);

      // then
      assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    @DisplayName("0으로 곱하기")
    void multiplyByZero() {
      // given
      final Money money = Money.of(new BigDecimal("1000"));

      // when
      final Money result = money.multiply(0);

      // then
      assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("음수로 곱하기 시 예외 발생")
    void throwExceptionWhenMultiplyingByNegative() {
      // given
      final Money money = Money.of(new BigDecimal("1000"));

      // when & then
      assertThatThrownBy(() -> money.multiply(-5))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Multiplier cannot be negative: -5");
    }
  }

  @Nested
  @DisplayName("비교 연산 테스트")
  class ComparisonTests {

    @Test
    @DisplayName("같은 금액 비교")
    void compareEqualAmounts() {
      // given
      final Money money1 = Money.of(new BigDecimal("10000"));
      final Money money2 = Money.of(new BigDecimal("10000.00"));

      // when & then
      assertThat(money1.equals(money2)).isTrue();
      assertThat(money1.isGreaterThan(money2)).isFalse();
      assertThat(money1.isLessThan(money2)).isFalse();
    }

    @Test
    @DisplayName("큰 금액 비교")
    void compareGreaterAmount() {
      // given
      final Money money1 = Money.of(new BigDecimal("10000"));
      final Money money2 = Money.of(new BigDecimal("5000"));

      // when & then
      assertThat(money1.isGreaterThan(money2)).isTrue();
      assertThat(money1.isGreaterThanOrEqual(money2)).isTrue();
      assertThat(money1.isLessThan(money2)).isFalse();
    }

    @Test
    @DisplayName("작은 금액 비교")
    void compareLesserAmount() {
      // given
      final Money money1 = Money.of(new BigDecimal("5000"));
      final Money money2 = Money.of(new BigDecimal("10000"));

      // when & then
      assertThat(money1.isLessThan(money2)).isTrue();
      assertThat(money1.isGreaterThan(money2)).isFalse();
    }

    @Test
    @DisplayName("같거나 큰 금액 비교")
    void compareGreaterThanOrEqual() {
      // given
      final Money money1 = Money.of(new BigDecimal("10000"));
      final Money money2 = Money.of(new BigDecimal("10000"));
      final Money money3 = Money.of(new BigDecimal("5000"));

      // when & then
      assertThat(money1.isGreaterThanOrEqual(money2)).isTrue();
      assertThat(money1.isGreaterThanOrEqual(money3)).isTrue();
      assertThat(money3.isGreaterThanOrEqual(money1)).isFalse();
    }

    @Test
    @DisplayName("비교 시 null 파라미터 예외 발생")
    void throwExceptionWhenComparingWithNull() {
      // given
      final Money money = Money.of(new BigDecimal("10000"));

      // when & then
      assertThatThrownBy(() -> money.isGreaterThan(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Other money cannot be null");
    }
  }

  @Nested
  @DisplayName("equals 및 hashCode 테스트")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("같은 금액의 Money는 동일")
    void equalMoneyWithSameAmount() {
      // given
      final Money money1 = Money.of(new BigDecimal("10000"));
      final Money money2 = Money.of(new BigDecimal("10000.00"));

      // when & then
      assertThat(money1).isEqualTo(money2);
      assertThat(money1.hashCode()).isEqualTo(money2.hashCode());
    }

    @Test
    @DisplayName("다른 금액의 Money는 다름")
    void notEqualMoneyWithDifferentAmount() {
      // given
      final Money money1 = Money.of(new BigDecimal("10000"));
      final Money money2 = Money.of(new BigDecimal("5000"));

      // when & then
      assertThat(money1).isNotEqualTo(money2);
    }

    @Test
    @DisplayName("동일 객체는 동일")
    void equalsSameObject() {
      // given
      final Money money = Money.of(new BigDecimal("10000"));

      // when & then
      assertThat(money).isEqualTo(money);
    }

    @Test
    @DisplayName("null과 비교하면 다름")
    void notEqualsNull() {
      // given
      final Money money = Money.of(new BigDecimal("10000"));

      // when & then
      assertThat(money).isNotEqualTo(null);
    }

    @Test
    @DisplayName("다른 타입 객체와 비교하면 다름")
    void notEqualsDifferentType() {
      // given
      final Money money = Money.of(new BigDecimal("10000"));
      final String other = "10000";

      // when & then
      assertThat(money).isNotEqualTo(other);
    }
  }

  @Nested
  @DisplayName("toString 테스트")
  class ToStringTests {

    @Test
    @DisplayName("toString 형식 확인")
    void toStringFormat() {
      // given
      final Money money = Money.of(new BigDecimal("10000.50"));

      // when
      final String result = money.toString();

      // then
      assertThat(result).isEqualTo("10000.50");
    }
  }

  @Nested
  @DisplayName("isZero 테스트")
  class IsZeroTests {

    @Test
    @DisplayName("0 금액은 isZero가 true")
    void zeroMoneyIsZero() {
      // given
      final Money money = Money.ZERO;

      // when & then
      assertThat(money.isZero()).isTrue();
    }

    @Test
    @DisplayName("양수 금액은 isZero가 false")
    void positiveMoneyIsNotZero() {
      // given
      final Money money = Money.of(new BigDecimal("100"));

      // when & then
      assertThat(money.isZero()).isFalse();
    }
  }

  @Nested
  @DisplayName("엣지 케이스 및 경계값 테스트")
  class EdgeCaseTests {

    @Test
    @DisplayName("매우 큰 금액 (Long.MAX_VALUE) 생성 성공")
    void createMoneyWithVeryLargeAmount() {
      // given
      final long maxAmount = Long.MAX_VALUE;

      // when
      final Money money = Money.of(maxAmount);

      // then
      assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal(Long.MAX_VALUE));
    }

    @Test
    @DisplayName("매우 작은 소수 금액 (0.01) 생성 성공")
    void createMoneyWithVerySmallDecimal() {
      // given
      final BigDecimal smallAmount = new BigDecimal("0.01");

      // when
      final Money money = Money.of(smallAmount);

      // then
      assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("0.01"));
    }

    @Test
    @DisplayName("소수점 3자리 금액은 2자리로 반올림")
    void roundThreeDecimalPlacesToTwo() {
      // given
      final BigDecimal amount1 = new BigDecimal("100.123");
      final BigDecimal amount2 = new BigDecimal("100.126");

      // when
      final Money money1 = Money.of(amount1);
      final Money money2 = Money.of(amount2);

      // then
      assertThat(money1.getAmount()).isEqualByComparingTo(new BigDecimal("100.12"));
      assertThat(money2.getAmount()).isEqualByComparingTo(new BigDecimal("100.13"));
    }

    @Test
    @DisplayName("매우 큰 금액끼리 더하기")
    void addVeryLargeAmounts() {
      // given
      final Money money1 = Money.of(new BigDecimal("999999999999999"));
      final Money money2 = Money.of(new BigDecimal("1"));

      // when
      final Money result = money1.add(money2);

      // then
      assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("1000000000000000.00"));
    }

    @Test
    @DisplayName("매우 큰 금액에 큰 정수 곱하기")
    void multiplyVeryLargeAmountByLargeMultiplier() {
      // given
      final Money money = Money.of(new BigDecimal("1000000"));
      final int multiplier = 10000;

      // when
      final Money result = money.multiply(multiplier);

      // then
      assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("10000000000.00"));
    }

    @Test
    @DisplayName("1원 미만의 금액 (0.001) 반올림 확인")
    void roundSubWonAmount() {
      // given
      final BigDecimal subWonAmount = new BigDecimal("0.001");

      // when
      final Money money = Money.of(subWonAmount);

      // then
      assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("반올림 경계값 테스트 (0.005 -> 0.01)")
    void roundingBoundaryValue() {
      // given
      final BigDecimal boundaryAmount = new BigDecimal("0.005");

      // when
      final Money money = Money.of(boundaryAmount);

      // then
      assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("0.01"));
    }

    @Test
    @DisplayName("빼기 결과가 정확히 0인 경우")
    void subtractResultingInExactlyZero() {
      // given
      final Money money1 = Money.of(new BigDecimal("1000.50"));
      final Money money2 = Money.of(new BigDecimal("1000.50"));

      // when
      final Money result = money1.subtract(money2);

      // then
      assertThat(result.isZero()).isTrue();
      assertThat(result).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("1 곱하기는 원래 금액과 동일")
    void multiplyByOneReturnsSameAmount() {
      // given
      final Money money = Money.of(new BigDecimal("12345.67"));

      // when
      final Money result = money.multiply(1);

      // then
      assertThat(result).isEqualTo(money);
    }

    @Test
    @DisplayName("소수점이 있는 금액끼리 더하기 후 반올림 확인")
    void addDecimalAmountsAndVerifyRounding() {
      // given
      final Money money1 = Money.of(new BigDecimal("10.555"));
      final Money money2 = Money.of(new BigDecimal("20.445"));

      // when
      final Money result = money1.add(money2);

      // then
      // 10.56 + 20.45 = 31.01
      assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("31.01"));
    }

    @Test
    @DisplayName("매우 작은 금액끼리 비교")
    void compareTinyAmounts() {
      // given
      final Money money1 = Money.of(new BigDecimal("0.01"));
      final Money money2 = Money.of(new BigDecimal("0.02"));

      // when & then
      assertThat(money1.isLessThan(money2)).isTrue();
      assertThat(money2.isGreaterThan(money1)).isTrue();
    }
  }
}
