package com.teambind.springproject.integration.e2e;

import com.teambind.springproject.integration.IntegrationTestContainers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test to verify integration test infrastructure is working.
 */
@DisplayName("Integration 인프라 검증 테스트")
class E2ESmokeTest extends BaseE2ETest {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	@DisplayName("Testcontainers가 정상적으로 실행되는지 확인")
	void testContainersAreRunning() {
		assertThat(IntegrationTestContainers.getPostgresContainer().isRunning())
				.as("PostgreSQL container should be running")
				.isTrue();

		assertThat(IntegrationTestContainers.getKafkaContainer().isRunning())
				.as("Kafka container should be running")
				.isTrue();
	}
	
	@Test
	@DisplayName("Spring Context가 정상적으로 로딩되는지 확인")
	void testContextLoads() {
		assertThat(applicationContext)
				.as("Application context should be loaded")
				.isNotNull();
		
		assertThat(restTemplate)
				.as("TestRestTemplate should be autowired")
				.isNotNull();
		
		assertThat(kafkaTemplate)
				.as("KafkaTemplate should be autowired")
				.isNotNull();
	}
	
	@Test
	@DisplayName("데이터베이스 연결이 정상적으로 동작하는지 확인")
	void testDatabaseConnection() {
		assertThat(IntegrationTestContainers.getPostgresContainer().getJdbcUrl())
				.as("Database URL should be configured")
				.isNotBlank();

		assertThat(IntegrationTestContainers.getPostgresContainer().getUsername())
				.as("Database username should be configured")
				.isEqualTo("test");
	}

	@Test
	@DisplayName("Kafka 브로커가 정상적으로 실행되는지 확인")
	void testKafkaConnection() {
		assertThat(IntegrationTestContainers.getKafkaContainer().getBootstrapServers())
				.as("Kafka bootstrap servers should be configured")
				.isNotBlank();
	}
	
	@Test
	@DisplayName("API 엔드포인트에 접근 가능한지 확인")
	void testApiEndpointAccessible() {
		final String baseUrl = getBaseUrl();
		assertThat(baseUrl)
				.as("Base URL should be configured")
				.startsWith("http://localhost:");
		
		assertThat(port)
				.as("Port should be assigned")
				.isGreaterThan(0);
	}
}
