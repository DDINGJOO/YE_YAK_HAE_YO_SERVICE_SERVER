package com.teambind.springproject.performance.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Integration 성능 테스트를 위한 Base 클래스.
 * <p>
 * Testcontainers PostgreSQL을 Singleton 패턴으로 사용하여 실제 PostgreSQL 환경에서 테스트합니다.
 * <p>
 * 모든 테스트 클래스가 동일한 컨테이너 인스턴스를 공유하므로
 * 테스트 간 격리를 보장하면서도 컨테이너 재시작 오버헤드를 방지합니다.
 * <p>
 * JVM heap과 분리된 Docker 컨테이너에서 데이터를 관리하므로
 * OutOfMemoryError를 방지하고 실제 환경과 동일한 성능 측정이 가능합니다.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Tag("integration")
public abstract class LargeScaleTestBase {
	
	/**
	 * Singleton PostgreSQL Container.
	 *
	 * 여러 테스트 클래스에서 공유되는 단일 컨테이너 인스턴스입니다.
	 * 컨테이너는 첫 번째 테스트 시작 시 한 번만 생성되고,
	 * 모든 테스트가 끝날 때까지 유지됩니다.
	 */
	protected static final PostgreSQLContainer<?> postgres;
	
	static {
		postgres = new PostgreSQLContainer<>("postgres:16-alpine")
				.withDatabaseName("performance_test_db")
				.withUsername("test")
				.withPassword("test")
				.withReuse(true);
		postgres.start();
	}
	
	@DynamicPropertySource
	static void configureProperties(final DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		
		// JPA 설정
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
		registry.add("spring.jpa.show-sql", () -> "false");
		registry.add("spring.jpa.properties.hibernate.format_sql", () -> "false");
		
		// Flyway 비활성화 (ddl-auto로 스키마 생성)
		registry.add("spring.flyway.enabled", () -> "false");
	}
}
