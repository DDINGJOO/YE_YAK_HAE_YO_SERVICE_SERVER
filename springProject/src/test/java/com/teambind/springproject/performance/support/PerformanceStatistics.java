package com.teambind.springproject.performance.support;

import java.util.List;

/**
 * 성능 측정 통계 계산 유틸리티.
 * <p>
 * 여러 번의 성능 측정 결과로부터 통계 수치를 계산합니다.
 */
public final class PerformanceStatistics {
	
	private PerformanceStatistics() {
		// Utility class
	}
	
	/**
	 * 평균 값을 계산합니다 (Long).
	 *
	 * @param values 값 리스트
	 * @return 평균 값
	 */
	public static double average(final List<Long> values) {
		if (values.isEmpty()) {
			return 0.0;
		}
		return values.stream()
				.mapToLong(Long::longValue)
				.average()
				.orElse(0.0);
	}
	
	/**
	 * 평균 값을 계산합니다 (Double).
	 *
	 * @param values 값 리스트
	 * @return 평균 값
	 */
	public static double averageDouble(final List<Double> values) {
		if (values.isEmpty()) {
			return 0.0;
		}
		return values.stream()
				.mapToDouble(Double::doubleValue)
				.average()
				.orElse(0.0);
	}
	
	/**
	 * 최소 값을 계산합니다.
	 *
	 * @param values 값 리스트
	 * @return 최소 값
	 */
	public static long min(final List<Long> values) {
		if (values.isEmpty()) {
			return 0L;
		}
		return values.stream()
				.mapToLong(Long::longValue)
				.min()
				.orElse(0L);
	}
	
	/**
	 * 최대 값을 계산합니다.
	 *
	 * @param values 값 리스트
	 * @return 최대 값
	 */
	public static long max(final List<Long> values) {
		if (values.isEmpty()) {
			return 0L;
		}
		return values.stream()
				.mapToLong(Long::longValue)
				.max()
				.orElse(0L);
	}
	
	/**
	 * 표준편차를 계산합니다.
	 *
	 * @param values 값 리스트
	 * @return 표준편차
	 */
	public static double standardDeviation(final List<Long> values) {
		if (values.isEmpty()) {
			return 0.0;
		}
		
		final double mean = average(values);
		final double variance = values.stream()
				.mapToDouble(value -> Math.pow(value - mean, 2))
				.average()
				.orElse(0.0);
		
		return Math.sqrt(variance);
	}
	
	/**
	 * 백분위수를 계산합니다.
	 *
	 * @param values     값 리스트
	 * @param percentile 백분위수 (0-100)
	 * @return 백분위수 값
	 */
	public static long percentile(final List<Long> values, final int percentile) {
		if (values.isEmpty()) {
			return 0L;
		}
		
		final List<Long> sorted = values.stream()
				.sorted()
				.toList();
		
		final int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
		return sorted.get(Math.max(0, index));
	}
}
