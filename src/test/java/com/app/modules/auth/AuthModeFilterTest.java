package com.app.modules.auth;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthModeFilterTest {

    private AuthModeFilter filter;
    private ContainerRequestContext requestContext;
    private UriInfo uriInfo;

    @BeforeEach
    void setup() {
        filter = new AuthModeFilter();
        requestContext = mock(ContainerRequestContext.class);
        uriInfo = mock(UriInfo.class);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
    }

    @Test
    void testLocalModeDoesNotBlockAuthPaths() {
        filter.mode = "local";
        when(uriInfo.getPath()).thenReturn("/v1/auth/login");

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
    }

    @Test
    void testLocalModeDoesNotBlockNonAuthPaths() {
        filter.mode = "local";
        when(uriInfo.getPath()).thenReturn("/v1/user");

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
    }

    @Test
    void testRemoteModeBlocksAuthPathsWith404() {
        filter.mode = "remote";
        when(uriInfo.getPath()).thenReturn("/v1/auth/login");

        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(r -> r.getStatus() == 404));
    }

    @Test
    void testRemoteModeBlocksAllAuthSubpaths() {
        filter.mode = "remote";
        when(uriInfo.getPath()).thenReturn("/v1/auth/me");

        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(r -> r.getStatus() == 404));
    }

    @Test
    void testRemoteModeDoesNotBlockHealthPath() {
        filter.mode = "remote";
        when(uriInfo.getPath()).thenReturn("/v1/health");

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
    }

    @Test
    void testRemoteModeDoesNotBlockUserPath() {
        filter.mode = "remote";
        when(uriInfo.getPath()).thenReturn("/v1/user");

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
    }

    @Test
    void testRemoteModeDoesNotBlockRoot() {
        filter.mode = "remote";
        when(uriInfo.getPath()).thenReturn("/");

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
    }
}
