package org.pinnel.pinnelapi.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method parameter that should be resolved to the current
 * {@link org.pinnel.pinnelapi.entity.UserEntity} populated by {@link CognitoAuthFilter}.
 * Returns 401 if the request did not carry a recognized Cognito id header.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
