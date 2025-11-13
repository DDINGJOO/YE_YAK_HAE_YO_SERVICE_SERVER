package com.teambind.springproject;

import com.teambind.springproject.application.port.out.publisher.EventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
@ImportAutoConfiguration(exclude = {FlywayAutoConfiguration.class, KafkaAutoConfiguration.class})
class SpringProjectApplicationTests {

	@MockBean
	private EventPublisher eventPublisher;

	@Test
	void contextLoads() {
	}

}
