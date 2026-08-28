package com.example.syntricdb.openapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI syntricDbOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SyntricDB AI-Native Database — Spring Boot 3 OpenAPI Specification")
                        .description("Interactive REST API documentation for SyntricDB Spring Boot 3 JPA integration, HNSW Vector Similarity Search, and Native In-Engine AI RAG.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Upendra Manike / SyntricDB Team")
                                .email("upendra@syntricdb.com")
                                .url("https://syntricdb.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8082").description("Spring Boot OpenAPI App"),
                        new Server().url("http://localhost:8080").description("SyntricDB Engine Web Server")
                ));
    }
}
