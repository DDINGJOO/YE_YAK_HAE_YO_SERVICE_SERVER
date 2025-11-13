package com.teambind.springproject.integration;

import org.flywaydb.core.Flyway;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton Testcontainers for integration tests.
 * All integration test classes share the same container instances.
 */
public class IntegrationTestContainers {

	private static final PostgreSQLContainer<?> postgresContainer;
	private static final KafkaContainer kafkaContainer;

	static {
		postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
				.withDatabaseName("testdb")
				.withUsername("test")
				.withPassword("test");
		postgresContainer.start();

		// Run Flyway migrations once when container starts
		final Flyway flyway = Flyway.configure()
				.dataSource(
						postgresContainer.getJdbcUrl(),
						postgresContainer.getUsername(),
						postgresContainer.getPassword()
				)
				.locations("classpath:db/migration")
				.cleanDisabled(false)
				.load();

		// Clean database and run migrations
		flyway.clean();
		flyway.migrate();

		kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
		kafkaContainer.start();

		// Add shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			postgresContainer.stop();
			kafkaContainer.stop();
		}));
	}
	
	public static PostgreSQLContainer<?> getPostgresContainer() {
		return postgresContainer;
	}
	
	public static KafkaContainer getKafkaContainer() {
		return kafkaContainer;
	}
}
