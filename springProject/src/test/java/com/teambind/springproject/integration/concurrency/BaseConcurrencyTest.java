package com.teambind.springproject.integration.concurrency;

import com.teambind.springproject.integration.IntegrationTestContainers;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
public abstract class BaseConcurrencyTest {

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	protected void cleanDatabase() {
		// Clean all data before each test and reset sequences
		jdbcTemplate.execute("TRUNCATE TABLE reservation_pricing_products, reservation_pricing_slots, reservation_pricings, time_range_prices, pricing_policies, room_allowed_products, product_time_slot_inventory, products RESTART IDENTITY CASCADE");
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
