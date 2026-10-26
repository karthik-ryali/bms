package com.banking.virtualwallet.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 / Swagger UI configuration for BMS REST APIs.
 */
@Configuration
public class OpenApiConfig {

    private static final String BASIC_AUTH_SCHEME = "basicAuth";

    @Bean
    public OpenAPI virtualWalletOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Online Banking System with Virtual Wallet Management API")
                        .description("RESTful API documentation for Virtual Wallet Management, Multi-Tier Overflow Transactions, and Probability & Statistics (P&S) Analytics Engine.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("R.V.S.V. Karthik & Team")
                                .email("karthik@banking.com"))
                        .license(new License().name("Educational Use").url("https://spring.io")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BASIC_AUTH_SCHEME, new SecurityScheme()
                                .name(BASIC_AUTH_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("Basic Authentication with username and password (e.g. karthik / password123)")));
    }
}
