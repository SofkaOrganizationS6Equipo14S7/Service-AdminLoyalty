package com.loyalty.service_admin.infrastructure.security;

import com.loyalty.service_admin.application.service.PermissionService;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Aspect que intercepta métodos anotados con @RequirePermission y valida permisos.
 * SPEC-004 RN-04: Validar permisos granulares en cada request.
 * 
 * Flujo:
 * 1. Controlador recibe request
 * 2. Spring intercepta método anotado con @RequirePermission
 * 3. PermissionAspect extrae annotation y llama a PermissionService
 * 4. Si usuario NO tiene permiso → 403 Forbidden
 * 5. Si usuario SÍ tiene permiso → ejecuta el método original
 * 
 * Ejemplo de uso en controlador:
 * 
 * @RequirePermission("promotion:write")
 * @PostMapping("/promotions")
 * public ResponseEntity<PromoResponse> create(...) { 
 *     // Código aquí solo se ejecuta si usuario tiene "promotion:write"
 * }
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionAspect {
    
    private final PermissionService permissionService;
    private final SecurityContextHelper securityContextHelper;
    
    /**
     * Intercepta llamadas a métodos anotados con @RequirePermission.
     * 
     * @param joinPoint punto de ejecución (método interceptado)
     * @param requirePermission anotación con permisos requeridos
     * @return resultado de la ejecución del método si validación pasa
     * @throws AuthorizationException si usuario no tiene permisos requeridos
     */
    @Around("@annotation(requirePermission)")
    public Object validatePermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        String[] requiredPermissions = requirePermission.value();
        
        // Si no se especifican permisos, permitir acceso
        if (requiredPermissions.length == 0) {
            return joinPoint.proceed();
        }
        
        // Obtener información del método para logging
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        
        // Validar permisos  según lógica requireAll
        boolean hasPermission = validatePermissions(requiredPermissions, requirePermission.requireAll());
        
        if (!hasPermission) {
            String userUid = securityContextHelper.getCurrentUserUid().toString();
            String requiredPermsStr = String.join(", ", requiredPermissions);
            log.warn("Acceso denegado: usuario {} intenta acceder a {} sin permisos. Requeridos: {}",
                    userUid, methodName, requiredPermsStr);
            
            throw new AuthorizationException(
                    String.format("No tienes permiso para ejecutar esta acción. Requeridos: %s", requiredPermsStr)
            );
        }
        
        log.debug("Acceso permitido: usuario {} ejecutando {}. Permisos validados: {}",
                securityContextHelper.getCurrentUserUid(), methodName, String.join(", ", requiredPermissions));
        
        return joinPoint.proceed();
    }
    
    /**
     * Valida si el usuario actual tiene los permisos requeridos.
     * 
     * @param permissions array de códigos de permiso
     * @param requireAll si true valida TODOS, si false valida ALGUNO
     * @return true si validación pasa, false en caso contrario
     */
    private boolean validatePermissions(String[] permissions, boolean requireAll) {
        if (requireAll) {
            return permissionService.hasAllPermissions(permissions);
        } else {
            return permissionService.hasAnyPermission(permissions);
        }
    }
}
