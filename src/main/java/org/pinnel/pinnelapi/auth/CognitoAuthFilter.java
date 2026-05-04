package org.pinnel.pinnelapi.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.pinnel.pinnelapi.entity.UserEntity;
import org.pinnel.pinnelapi.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves the current user from API Gateway / Cognito headers on every request.
 * If a Cognito id header is present, looks up (or just-in-time creates) the user
 * and stores the {@link UserEntity} as a request attribute under
 * {@link #CURRENT_USER_ATTRIBUTE}, which {@link CurrentUserArgumentResolver}
 * later injects into controller methods.
 */
@Component
@RequiredArgsConstructor
public class CognitoAuthFilter extends OncePerRequestFilter {

    public static final String CURRENT_USER_ATTRIBUTE = "currentUser";

    private final UserService userService;
    private final CognitoHeadersProperties headers;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cognitoId = request.getHeader(headers.getCognitoId());
        if (cognitoId != null && !cognitoId.isBlank()) {
            UserEntity user = userService.findOrCreateByCognitoId(
                    cognitoId,
                    request.getHeader(headers.getEmail()),
                    request.getHeader(headers.getUsername())
            );
            request.setAttribute(CURRENT_USER_ATTRIBUTE, user);
        }
        chain.doFilter(request, response);
    }
}
