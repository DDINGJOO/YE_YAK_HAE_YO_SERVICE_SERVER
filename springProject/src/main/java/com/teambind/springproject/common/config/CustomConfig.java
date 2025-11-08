package com.teambind.springproject.common.config;

import com.teambind.springproject.common.util.generator.PrimaryKeyGenerator;
import com.teambind.springproject.common.util.generator.Snowflake;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomConfig {
	
	@Bean
	public PrimaryKeyGenerator PkeyGenerator() {
		return new Snowflake();
	}
}
