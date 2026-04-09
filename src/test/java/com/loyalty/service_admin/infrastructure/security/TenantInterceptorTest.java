package com.loyalty.service_admin.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantInterceptor Unit Tests")
class TenantInterceptorTest {

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private TenantInterceptor tenantInterceptor;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void testPreHandle_authenticatedUser_setsTenant() {
        // Arrange
        UUID ecommerceId = UUID.randomUUID();
        UUID uid = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(uid, "user", "pass", "STORE_ADMIN", ecommerceId, true);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(ecommerceId);

        // Act
        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        // Assert
        assertTrue(result);
        assertEquals(ecommerceId, TenantContext.getCurrentTenant());
    }

    @Test
    void testPreHandle_superAdmin_noTenantSet() {
        // Arrange
        UUID uid = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(uid, "admin", "pass", "SUPER_ADMIN", null, true);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenReturn(null);

        // Act
        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        // Assert
        assertTrue(result);
        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    void testPreHandle_noAuthentication_returnsTrue() {
        // Arrange - no authentication
        SecurityContextHolder.clearContext();

        // Act
        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        // Assert
        assertTrue(result);
    }

    @Test
    void testPreHandle_nonUserPrincipal_returnsTrue() {
        // Arrange
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("stringPrincipal", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        // Assert
        assertTrue(result);
    }

    @Test
    void testPreHandle_exception_returnsTrue() {
        // Arrange
        UUID uid = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(uid, "user", "pass", "ADMIN", UUID.randomUUID(), true);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(securityContextHelper.getCurrentUserEcommerceId()).thenThrow(new RuntimeException("error"));

        // Act
        boolean result = tenantInterceptor.preHandle(request, response, new Object());

        // Assert
        assertTrue(result);
    }

    @Test
    void testAfterCompletion_clearsTenantContext() {
        // Arrange
        TenantContext.setCurrentTenant(UUID.randomUUID());

        // Act
        tenantInterceptor.afterCompletion(request, response, new Object(), null);

        // Assert
        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    void testAfterCompletion_withException_stillClears() {
        // Arrange
        TenantContext.setCurrentTenant(UUID.randomUUID());

        // Act
        tenantInterceptor.afterCompletion(request, response, new Object(), new Exception("test error"));

        // Assert
        assertNull(TenantContext.getCurrentTenant());
    }
}
