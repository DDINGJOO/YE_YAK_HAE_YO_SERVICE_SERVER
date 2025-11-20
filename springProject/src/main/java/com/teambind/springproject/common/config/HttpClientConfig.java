package com.teambind.springproject.common.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * HTTP 클라이언트 설정.
 * RestTemplate Bean을 생성하고 타임아웃 등의 기본 설정을 적용합니다.
 */
@Configuration
public class HttpClientConfig {

	private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(5);
	private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

	/**
	 * RestTemplate Bean 생성.
	 * 외부 서비스와의 HTTP 통신에 사용됩니다.
	 *
	 * @param restTemplateBuilder RestTemplate 빌더
	 * @return 설정이 적용된 RestTemplate
	 */
	@Bean
	public RestTemplate restTemplate(final RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder
				.requestFactory(this::clientHttpRequestFactory)
				.setConnectTimeout(CONNECTION_TIMEOUT)
				.setReadTimeout(READ_TIMEOUT)
				.build();
	}

	/**
	 * HTTP 요청 팩토리 생성.
	 * 커넥션 타임아웃과 읽기 타임아웃을 설정합니다.
	 *
	 * @return ClientHttpRequestFactory
	 */
	private ClientHttpRequestFactory clientHttpRequestFactory() {
		final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout((int) CONNECTION_TIMEOUT.toMillis());
		factory.setReadTimeout((int) READ_TIMEOUT.toMillis());
		return factory;
	}
}