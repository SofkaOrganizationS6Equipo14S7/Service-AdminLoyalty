package com.loyalty.service_admin.infrastructure.security;

import com.loyalty.service_admin.application.service.PermissionService;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionAspect Unit Tests")
class PermissionAspectTest {

    @Mock
    private PermissionService permissionService;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private PermissionAspect permissionAspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Test
    void testValidatePermission_noPermissionsRequired_allowsAccess() throws Throwable {
        // Arrange
        RequirePermission annotation = mock(RequirePermission.class);
        when(annotation.value()).thenReturn(new String[]{});
        when(joinPoint.proceed()).thenReturn("result");

        // Act
        Object result = permissionAspect.validatePermission(joinPoint, annotation);

        // Assert
        assertEquals("result", result);
        verify(joinPoint).proceed();
    }

    @Test
    void testValidatePermission_hasAllPermissions_allowsAccess() throws Throwable {
        // Arrange
        RequirePermission annotation = mock(RequirePermission.class);
        when(annotation.value()).thenReturn(new String[]{"user:read", "user:write"});
        when(annotation.requireAll()).thenReturn(true);
        when(permissionService.hasAllPermissions(new String[]{"user:read", "user:write"})).thenReturn(true);
        when(securityContextHelper.getCurrentUserUid()).thenReturn(UUID.randomUUID());
        
        Method method = PermissionAspectTest.class.getMethod("dummyMethod");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenReturn("ok");

        // Act
        Object result = permissionAspect.validatePermission(joinPoint, annotation);

        // Assert
        assertEquals("ok", result);
    }

    @Test
    void testValidatePermission_hasAnyPermission_allowsAccess() throws Throwable {
        // Arrange
        RequirePermission annotation = mock(RequirePermission.class);
        when(annotation.value()).thenReturn(new String[]{"user:read"});
        when(annotation.requireAll()).thenReturn(false);
        when(permissionService.hasAnyPermission(new String[]{"user:read"})).thenReturn(true);
        when(securityContextHelper.getCurrentUserUid()).thenReturn(UUID.randomUUID());
        
        Method method = PermissionAspectTest.class.getMethod("dummyMethod");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenReturn("ok");

        // Act
        Object result = permissionAspect.validatePermission(joinPoint, annotation);

        // Assert
        assertEquals("ok", result);
    }

    @Test
    void testValidatePermission_noPermission_throwsAuthorizationException() throws Throwable {
        // Arrange
        RequirePermission annotation = mock(RequirePermission.class);
        when(annotation.value()).thenReturn(new String[]{"admin:delete"});
        when(annotation.requireAll()).thenReturn(true);
        when(permissionService.hasAllPermissions(new String[]{"admin:delete"})).thenReturn(false);
        when(securityContextHelper.getCurrentUserUid()).thenReturn(UUID.randomUUID());
        
        Method method = PermissionAspectTest.class.getMethod("dummyMethod");
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);

        // Act & Assert
        assertThrows(AuthorizationException.class, 
            () -> permissionAspect.validatePermission(joinPoint, annotation));
    }

    // Dummy method for reflection
    public void dummyMethod() {}
}
