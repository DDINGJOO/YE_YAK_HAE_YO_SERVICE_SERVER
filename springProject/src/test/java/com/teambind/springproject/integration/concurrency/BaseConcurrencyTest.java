package com.teambind.springproject.integration.concurrency;

import com.teambind.springproject.application.port.out.publisher.EventPublisher;
import com.teambind.springproject.integration.IntegrationTestContainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for concurrency tests using shared IntegrationTestContainers.
 * Reuses IntegrationTestContainers singleton for PostgreSQL and Kafka.
 */
@SpringBootTest
@ActiveProfiles("integration")
@Tag("integration")
public abstract class BaseConcurrencyTest {

	@MockBean
	protected EventPublisher eventPublisher;

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	protected void cleanDatabase() {
		// Clean all data before each test
		// Using DELETE instead of TRUNCATE to avoid implicit commits that break connection pool
		jdbcTemplate.execute("DELETE FROM reservation_pricing_products");
		jdbcTemplate.execute("DELETE FROM reservation_pricing_slots");
		jdbcTemplate.execute("DELETE FROM reservation_pricings");
		jdbcTemplate.execute("DELETE FROM time_range_prices");
		jdbcTemplate.execute("DELETE FROM pricing_policies");
		jdbcTemplate.execute("DELETE FROM room_allowed_products");
		jdbcTemplate.execute("DELETE FROM product_time_slot_inventory");
		jdbcTemplate.execute("DELETE FROM products");

		// NOTE: No sequence reset needed - using Snowflake ID (V5 migration)
	}

	// Reuse shared containers from integration test infrastructure
	protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
			IntegrationTestContainers.getPostgresContainer();
	protected static final KafkaContainer KAFKA_CONTAINER =
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
