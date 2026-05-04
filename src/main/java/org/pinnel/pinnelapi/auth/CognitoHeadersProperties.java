package org.pinnel.pinnelapi.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Names of the HTTP headers that API Gateway forwards from the Cognito authorizer. Configurable via {@code pinnel.auth.headers.*}. */
@ConfigurationProperties(prefix = "pinnel.auth.headers")
@Getter
@Setter
public class CognitoHeadersProperties {

    private String cognitoId = "X-Cognito-Id";

    private String email = "X-Cognito-Email";

    private String username = "X-Cognito-Username";
}
