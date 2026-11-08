package dev.hbn.doodle.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

@Configuration
public class OpenApiConfig {

    static final String SORT_EXAMPLE = "id,asc";

    @Bean
    OperationCustomizer sortParameterExample() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            if (operation.getParameters() == null) {
                return operation;
            }
            for (Parameter parameter : operation.getParameters()) {
                if (!"sort".equals(parameter.getName()) || parameter.getSchema() == null) {
                    continue;
                }
                Schema<?> items = parameter.getSchema().getItems();
                if (items != null) {
                    items.setExample(SORT_EXAMPLE);
                }
            }
            return operation;
        };
    }
}
