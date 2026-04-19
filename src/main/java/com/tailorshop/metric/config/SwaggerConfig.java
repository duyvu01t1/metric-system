package com.tailorshop.metric.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI Configuration for API documentation
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Metric System - Tailoring Management API")
                .version("1.0.0")
                .description("REST API for managing tailoring shop operations, customer information, " +
                    "measurements, and orders. Supports role-based access control and OAuth2 authentication.")
                .contact(new Contact()
                    .name("Tailoring Management Team")
                    .email("support@tailorsystem.local")
                    .url("http://tailorsystem.local"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }

}
