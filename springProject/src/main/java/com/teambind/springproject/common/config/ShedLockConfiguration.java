package com.teambind.springproject.common.config;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * ShedLock 설정.
 * 분산 환경에서 스케줄러가 중복 실행되지 않도록 락을 관리합니다.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class ShedLockConfiguration {

  /**
   * JDBC 기반 LockProvider를 생성합니다.
   * shedlock 테이블을 사용하여 분산 락을 관리합니다.
   *
   * @param dataSource 데이터 소스
   * @return LockProvider
   */
  @Bean
  public LockProvider lockProvider(final DataSource dataSource) {
    return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
        .withJdbcTemplate(new JdbcTemplate(dataSource))
        .usingDbTime()
        .build()
    );
  }
}