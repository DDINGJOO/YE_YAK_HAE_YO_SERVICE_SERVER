package com.teambind.springproject.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 금액을 표현하는 Value Object.
 * BigDecimal을 사용하여 정확한 금액 계산을 보장합니다.
 */
public class Money {
	
	public static final Money ZERO = new Money(BigDecimal.ZERO);
	
	private final BigDecimal amount;
	
	private Money(final BigDecimal amount) {
		validateAmount(amount);
		this.amount = amount.setScale(2, RoundingMode.HALF_UP);
	}
	
	public static Money of(final BigDecimal amount) {
		if (amount == null) {
			throw new IllegalArgumentException("Amount cannot be null");
		}
		return new Money(amount);
	}
	
	public static Money of(final long amount) {
		return new Money(BigDecimal.valueOf(amount));
	}
	
	public static Money of(final double amount) {
		return new Money(BigDecimal.valueOf(amount));
	}
	
	public static Money zero() {
		return ZERO;
	}
	
	private void validateAmount(final BigDecimal amount) {
		if (amount == null) {
			throw new IllegalArgumentException("Amount cannot be null");
		}
		if (amount.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("Amount cannot be negative: " + amount);
		}
	}
	
	public Money add(final Money other) {
		if (other == null) {
			throw new IllegalArgumentException("Other money cannot be null");
		}
		return new Money(this.amount.add(other.amount));
	}
	
	public Money subtract(final Money other) {
		if (other == null) {
			throw new IllegalArgumentException("Other money cannot be null");
		}
		final BigDecimal result = this.amount.subtract(other.amount);
		if (result.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("Result cannot be negative");
		}
		return new Money(result);
	}
	
	public Money multiply(final int multiplier) {
		if (multiplier < 0) {
			throw new IllegalArgumentException("Multiplier cannot be negative: " + multiplier);
		}
		return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)));
	}
	
	public Money multiply(final BigDecimal multiplier) {
		if (multiplier == null) {
			throw new IllegalArgumentException("Multiplier cannot be null");
		}
		if (multiplier.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("Multiplier cannot be negative: " + multiplier);
		}
		return new Money(this.amount.multiply(multiplier));
	}
	
	public boolean isGreaterThan(final Money other) {
		if (other == null) {
			throw new IllegalArgumentException("Other money cannot be null");
		}
		return this.amount.compareTo(other.amount) > 0;
	}
	
	public boolean isGreaterThanOrEqual(final Money other) {
		if (other == null) {
			throw new IllegalArgumentException("Other money cannot be null");
		}
		return this.amount.compareTo(other.amount) >= 0;
	}
	
	public boolean isLessThan(final Money other) {
		if (other == null) {
			throw new IllegalArgumentException("Other money cannot be null");
		}
		return this.amount.compareTo(other.amount) < 0;
	}
	
	public boolean isZero() {
		return this.amount.compareTo(BigDecimal.ZERO) == 0;
	}
	
	public BigDecimal getAmount() {
		return amount;
	}
	
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final Money money = (Money) o;
		// Use compareTo instead of equals to ignore scale differences
		// Money.of(100.0) should equal Money.of(100.00)
		return this.amount.compareTo(money.amount) == 0;
	}

	@Override
	public int hashCode() {
		// compareTo와 일관성을 유지하기 위해 정규화된 값 사용
		// Money.of(100.0)과 Money.of(100.00)은 동일한 hashCode를 가져야 함

		// 0은 항상 동일한 hashCode
		if (amount.compareTo(BigDecimal.ZERO) == 0) {
			return Objects.hash(BigDecimal.ZERO);
		}

		// scale을 제거한 정규화된 값으로 hashCode 계산
		return Objects.hash(amount.stripTrailingZeros());
	}
	
	@Override
	public String toString() {
		return amount.toString();
	}
}
