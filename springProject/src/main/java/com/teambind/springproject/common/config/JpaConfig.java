package com.teambind.springproject.common.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * JPA 및 애플리케이션 타임존 설정.
 * 모든 날짜/시간 데이터를 한국 표준시(KST, Asia/Seoul)로 통일합니다.
 * 설정 범위:
 * JVM 기본 타임존: Asia/Seoul
 * JDBC 타임존: Asia/Seoul (application.yaml)
 * Hibernate 타임존: Asia/Seoul (application.yaml)
 * MySQL/MariaDB 세션 타임존: +09:00 (schema.sql)
 *
 */
@Configuration
public class JpaConfig {
	
	/**
	 * 애플리케이션 시작 시 JVM 기본 타임존을 Asia/Seoul로 설정한다.
	 * 이 설정은 다음에 영향을 줍니다:
	 * LocalDateTime.now() - 한국 시간 기준
	 * Instant.now() - UTC 기준 (변환 시 KST 사용)
	 * SimpleDateFormat - 기본 타임존 KST
	 *
	 */
	@PostConstruct
	public void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
	}
}
