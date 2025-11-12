package com.teambind.springproject.performance.support;

import org.hibernate.stat.Statistics;

import java.util.ArrayList;
import java.util.List;

/**
 * 성능 측정 결과 집계기.
 *
 * 여러 번의 측정 결과를 수집하고 통계를 계산합니다.
 */
public class PerformanceReportAggregator {
	
	private final Runtime runtime = Runtime.getRuntime();
	private final List<Long> queryCountList = new ArrayList<>();
	private final List<Long> durationList = new ArrayList<>();
	private final List<Double> memoryUsedList = new ArrayList<>();
	private long totalCacheHits = 0;
	private long totalCacheMisses = 0;
	
	/**
	 * 측정 결과를 추가합니다.
	 *
	 * @param queryCount 쿼리 개수
	 * @param duration   실행 시간 (ms)
	 */
	public void addMeasurement(final long queryCount, final long duration) {
		queryCountList.add(queryCount);
		durationList.add(duration);
		
		// 메모리 사용량 측정 (MB)
		final long usedMemory = runtime.totalMemory() - runtime.freeMemory();
		final double usedMemoryMB = usedMemory / (1024.0 * 1024.0);
		memoryUsedList.add(usedMemoryMB);
	}
	
	/**
	 * Hibernate Statistics 포함 측정 결과를 추가합니다.
	 *
	 * @param queryCount 쿼리 개수
	 * @param duration   실행 시간 (ms)
	 * @param statistics Hibernate Statistics
	 */
	public void addMeasurement(
			final long queryCount,
			final long duration,
			final Statistics statistics) {
		addMeasurement(queryCount, duration);
		
		// 캐시 히트/미스 누적
		totalCacheHits += statistics.getQueryCacheHitCount();
		totalCacheMisses += statistics.getQueryCacheMissCount();
	}
	
	/**
	 * 집계된 성능 리포트를 생성합니다.
	 *
	 * @return 집계된 성능 리포트
	 */
	public AggregatedPerformanceReport aggregate() {
		if (queryCountList.isEmpty()) {
			throw new IllegalStateException("No measurements collected");
		}
		
		final double avgQueryCount = PerformanceStatistics.average(queryCountList);
		final double avgDuration = PerformanceStatistics.average(durationList);
		final double avgMemory = PerformanceStatistics.averageDouble(memoryUsedList);
		
		// QPS (Queries Per Second) = 평균 쿼리 수 / (평균 실행 시간(초))
		final double qps = avgQueryCount / (avgDuration / 1000.0);
		
		// 쿼리당 평균 시간 (ms/query)
		final double avgTimePerQuery = avgQueryCount > 0 ? avgDuration / avgQueryCount : 0;
		
		// Throughput (operations/sec) = 초당 작업 수
		final double throughput = 1000.0 / avgDuration;
		
		// 캐시 히트율 (%)
		final long totalCacheAccess = totalCacheHits + totalCacheMisses;
		final double cacheHitRate = totalCacheAccess > 0
				? (totalCacheHits * 100.0) / totalCacheAccess
				: 0.0;
		
		return new AggregatedPerformanceReport(
				queryCountList.size(),
				avgQueryCount,
				PerformanceStatistics.min(queryCountList),
				PerformanceStatistics.max(queryCountList),
				avgDuration,
				PerformanceStatistics.min(durationList),
				PerformanceStatistics.max(durationList),
				PerformanceStatistics.standardDeviation(durationList),
				PerformanceStatistics.percentile(durationList, 50),
				PerformanceStatistics.percentile(durationList, 95),
				PerformanceStatistics.percentile(durationList, 99),
				qps,
				avgTimePerQuery,
				throughput,
				avgMemory,
				cacheHitRate
		);
	}
	
	/**
	 * 수집된 측정 횟수를 반환합니다.
	 */
	public int getMeasurementCount() {
		return queryCountList.size();
	}
}
