package com.teambind.springproject.performance.support;

/**
 * 집계된 성능 측정 결과.
 * <p>
 * 여러 번의 측정 결과를 통계적으로 집계한 리포트입니다.
 *
 * @param sampleCount 측정 횟수
 * @param avgQueryCount 평균 쿼리 개수
 * @param minQueryCount 최소 쿼리 개수
 * @param maxQueryCount 최대 쿼리 개수
 * @param avgDuration 평균 실행 시간 (ms)
 * @param minDuration 최소 실행 시간 (ms)
 * @param maxDuration 최대 실행 시간 (ms)
 * @param stdDevDuration 실행 시간 표준편차 (ms)
 * @param p50Duration P50 (중앙값) 실행 시간 (ms)
 * @param p95Duration P95 실행 시간 (ms)
 * @param p99Duration P99 실행 시간 (ms)
 * @param qps 초당 쿼리 수 (Queries Per Second)
 * @param avgTimePerQuery 쿼리당 평균 시간 (ms/query)
 * @param throughput 초당 처리량 (operations/sec)
 * @param memoryUsedMB 사용된 메모리 (MB)
 * @param cacheHitRate 캐시 히트율 (%)
 */
public record AggregatedPerformanceReport(
    int sampleCount,
    double avgQueryCount,
    long minQueryCount,
    long maxQueryCount,
    double avgDuration,
    long minDuration,
    long maxDuration,
    double stdDevDuration,
    long p50Duration,
    long p95Duration,
    long p99Duration,
    double qps,
    double avgTimePerQuery,
    double throughput,
    double memoryUsedMB,
    double cacheHitRate
) {

  /**
   * 콘솔 출력용 포맷팅된 문자열을 반환합니다.
   */
  public String toFormattedString() {
    return String.format("""
            Sample Count: %d

            Query Count:
              Average: %.2f
              Min: %d
              Max: %d

            Execution Time (ms):
              Average: %.2f ms
              Min: %d ms
              Max: %d ms
              Std Dev: %.2f ms
              P50: %d ms
              P95: %d ms
              P99: %d ms

            Performance Metrics:
              QPS (Queries/sec): %.2f
              Time per Query (ms/query): %.2f ms
              Throughput (ops/sec): %.2f
              Memory Used: %.2f MB
              Cache Hit Rate: %.2f%%
            """,
        sampleCount,
        avgQueryCount, minQueryCount, maxQueryCount,
        avgDuration, minDuration, maxDuration, stdDevDuration,
        p50Duration, p95Duration, p99Duration,
        qps, avgTimePerQuery, throughput, memoryUsedMB, cacheHitRate
    );
  }
}