package dev.hbn.doodle.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(present(properties.getAllowedOrigins()));
        configuration.setAllowedOriginPatterns(present(properties.getAllowedOriginPatterns()));
        configuration.setAllowedMethods(present(properties.getAllowedMethods()));
        configuration.setAllowedHeaders(present(properties.getAllowedHeaders()));
        configuration.setExposedHeaders(present(properties.getExposedHeaders()));
        configuration.setAllowCredentials(properties.getAllowCredentials());
        configuration.setAllowPrivateNetwork(properties.getAllowPrivateNetwork());
        configuration.setMaxAge(properties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        for (String pathPattern : present(properties.getPathPatterns())) {
            source.registerCorsConfiguration(pathPattern, configuration);
        }
        return source;
    }

    private static List<String> present(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().filter(StringUtils::hasText).toList();
    }
}
