package com.teambind.springproject.e2e;

import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton Testcontainers for E2E tests.
 * All test classes share the same container instances.
 */
public class E2ETestContainers {
	
	private static final PostgreSQLContainer<?> postgresContainer;
	private static final KafkaContainer kafkaContainer;
	
	static {
		postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
				.withDatabaseName("testdb")
				.withUsername("test")
				.withPassword("test");
		postgresContainer.start();
		
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
