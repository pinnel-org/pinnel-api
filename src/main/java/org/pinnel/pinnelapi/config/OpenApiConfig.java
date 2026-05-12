package org.pinnel.pinnelapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.pinnel.pinnelapi.auth.CurrentUser;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;

@Configuration
public class OpenApiConfig {

    private static final String COGNITO_ID_SCHEME = "cognitoId";

    @Bean
    public OpenAPI pinnelOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pinnel API")
                        .description("Backend for Pinnel — a vibe-planning travel app. "
                                + "Endpoints under /api/me, /api/cities, /api/pins, /api/trips. "
                                + "Authenticated endpoints expect the X-Cognito-Id header forwarded by API Gateway; "
                                + "for local dev, send it (and optionally X-Cognito-Email / X-Cognito-Username) by hand.")
                        .version("0.0.1-SNAPSHOT"))
                .components(new Components()
                        .addSecuritySchemes(COGNITO_ID_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Cognito-Id")
                                .description("Cognito subject id forwarded by API Gateway after Cognito authentication.")));
    }

    /**
     * Per-operation customization: handler methods with a {@link CurrentUser} parameter get
     * the {@code cognitoId} security requirement (so Swagger UI shows the lock icon and prompts
     * for the header in "Try it out"), and the resolver-supplied parameter itself is stripped
     * from the rendered operation so it does not appear as a spurious query/body field.
     */
    @Bean
    public OperationCustomizer currentUserOperationCustomizer() {
        return (operation, handlerMethod) -> {
            boolean authenticated = false;
            for (MethodParameter param : handlerMethod.getMethodParameters()) {
                if (param.hasParameterAnnotation(CurrentUser.class)) {
                    authenticated = true;
                    String name = param.getParameterName();
                    if (name != null && operation.getParameters() != null) {
                        operation.getParameters().removeIf(p -> name.equals(p.getName()));
                    }
                }
            }
            if (authenticated) {
                operation.addSecurityItem(new SecurityRequirement().addList(COGNITO_ID_SCHEME));
            }
            return operation;
        };
    }
}
