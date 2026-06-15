package com.app.modules.auth;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
@Priority(Priorities.AUTHENTICATION - 100)
public class AuthModeFilter implements ContainerRequestFilter {

    @Inject
    @ConfigProperty(name = "app.auth.mode", defaultValue = "local")
    String mode;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!"remote".equals(mode)) {
            return;
        }

        String path = requestContext.getUriInfo().getPath();
        if (path.startsWith("/v1/auth")) {
            requestContext.abortWith(Response.status(Response.Status.NOT_FOUND).build());
        }
    }
}
