package com.teambind.springproject;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@ImportAutoConfiguration(exclude = {FlywayAutoConfiguration.class, KafkaAutoConfiguration.class})
class SpringProjectApplicationTests {
	
	@Test
	void contextLoads() {
	}
	
}
