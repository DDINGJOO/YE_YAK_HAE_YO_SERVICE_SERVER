package com.teambind.springproject.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI reservationPricingOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Reservation Pricing Service API")
            .description("시간대별 가격 정책 및 예약 가격 계산 API")
            .version("v1.0.0")
            .contact(new Contact()
                .name("Team Bind")
                .email("ddingsha9@teambind.co.kr")))
        .servers(List.of(
            new Server()
                .url("http://localhost:8080")
                .description("Local Development Server"),
            new Server()
                .url("https://api.example.com")
                .description("Production Server")
        ));
  }
}
