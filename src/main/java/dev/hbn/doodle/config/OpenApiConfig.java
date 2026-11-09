package dev.hbn.doodle.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.*;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

@Configuration
public class OpenApiConfig {

    static final String SORT_EXAMPLE = "id,asc";
    static final String OAUTH_SCHEME = "keycloak";

    @Bean
    OpenAPI openAPI(@Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}") String issuer) {
        String realmIssuer = issuer.replaceAll("/+$", "");
        OAuthFlow authorizationCode = new OAuthFlow()
                .authorizationUrl(realmIssuer + "/protocol/openid-connect/auth")
                .tokenUrl(realmIssuer + "/protocol/openid-connect/token")
                .scopes(new Scopes()
                        .addString("openid", "OpenID Connect")
                        .addString("profile", "Read profile information")
                        .addString("email", "Read email address"));

        return new OpenAPI()
                .components(new Components().addSecuritySchemes(OAUTH_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .flows(new OAuthFlows().authorizationCode(authorizationCode))))
                .addSecurityItem(new SecurityRequirement().addList(OAUTH_SCHEME,
                        java.util.List.of("openid", "profile", "email")));
    }

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
