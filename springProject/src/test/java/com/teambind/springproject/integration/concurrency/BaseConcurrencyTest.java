package com.teambind.springproject.integration.concurrency;

import com.teambind.springproject.integration.IntegrationTestContainers;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for concurrency tests using shared IntegrationTestContainers.
 * Reuses IntegrationTestContainers singleton for PostgreSQL and Kafka.
 */
@SpringBootTest
@ActiveProfiles("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class BaseConcurrencyTest {

	// Reuse shared containers from integration test infrastructure
	protected static final org.testcontainers.containers.PostgreSQLContainer<?> POSTGRES_CONTAINER =
			IntegrationTestContainers.getPostgresContainer();
	protected static final org.testcontainers.containers.KafkaContainer KAFKA_CONTAINER =
			IntegrationTestContainers.getKafkaContainer();

	@DynamicPropertySource
	static void configureProperties(final DynamicPropertyRegistry registry) {
		// PostgreSQL properties
		registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
		registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);

		// Kafka properties
		registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
	}
}