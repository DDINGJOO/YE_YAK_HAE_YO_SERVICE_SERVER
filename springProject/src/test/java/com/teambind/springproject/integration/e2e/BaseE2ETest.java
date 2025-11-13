package com.teambind.springproject.integration.e2e;

import com.teambind.springproject.application.port.out.publisher.EventPublisher;
import com.teambind.springproject.integration.IntegrationTestContainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for End-to-End tests using Testcontainers.
 * Provides PostgreSQL and Kafka containers for realistic testing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@Tag("integration")
public abstract class BaseE2ETest {

	@MockBean
	protected EventPublisher eventPublisher;

	// Force early initialization of containers
	protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
			IntegrationTestContainers.getPostgresContainer();
	protected static final KafkaContainer KAFKA_CONTAINER =
			IntegrationTestContainers.getKafkaContainer();

	@LocalServerPort
	protected int port;

	@Autowired
	protected TestRestTemplate restTemplate;

	@Autowired
	protected KafkaTemplate<String, Object> kafkaTemplate;

	@Autowired
	protected JdbcTemplate jdbcTemplate;
	
	@DynamicPropertySource
	static void configureProperties(final DynamicPropertyRegistry registry) {
		// PostgreSQL properties
		registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
		registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
		
		// Kafka properties
		registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
	}
	
	protected String getBaseUrl() {
		return "http://localhost:" + port;
	}

	@BeforeEach
	void setUp() {
		// Common setup for all E2E tests can be added here
	}

	/**
	 * Clean all test data from database.
	 * Using DELETE instead of TRUNCATE to avoid implicit commits that break connection pool.
	 */
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
}
