package com.doodle.scheduling.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI doodleOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Doodle Scheduling API")
                .version("v1")
                .description("""
                        Publish free time as slots, then book one of those slots as a meeting.
                        Time that has no slot is neither free nor busy. All timestamps are UTC.
                        """));
    }
}
