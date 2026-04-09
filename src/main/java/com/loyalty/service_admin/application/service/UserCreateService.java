package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.dto.user.UserCreateRequest;
import com.loyalty.service_admin.application.dto.user.UserResponse;
import com.loyalty.service_admin.application.port.in.UserCreateUseCase;
import com.loyalty.service_admin.application.port.out.UserCreatePersistencePort;
import com.loyalty.service_admin.domain.entity.RoleEntity;
import com.loyalty.service_admin.domain.entity.UserEntity;
import com.loyalty.service_admin.domain.repository.RoleRepository;
import com.loyalty.service_admin.infrastructure.exception.AuthorizationException;
import com.loyalty.service_admin.infrastructure.exception.BadRequestException;
import com.loyalty.service_admin.infrastructure.exception.ConflictException;
import com.loyalty.service_admin.infrastructure.exception.UnauthorizedException;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementación del caso de uso: Creación de Usuarios.
 *
 * Responsabilidades:
 * - Validar autorización (rol, ecommerce)
 * - Validar unicidad de email y username
 * - Validar existencia de role y ecommerce
 * - Hash password usando BCrypt
 * - Registrar auditoría de creación
 * - Retornar UserResponse
 *
 * Patrón: Inyecta puertos (NO implementaciones concretas).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserCreateService implements UserCreateUseCase {
    
    private final UserCreatePersistencePort userCreatePersistencePort;
    private final SecurityContextHelper securityContextHelper;
    private final RoleRepository roleRepository;
    private final EcommerceService ecommerceService;
    private final AuditService auditService;
    private final PasswordValidator passwordValidator;
    private final BCryptPasswordEncoder passwordEncoder;
    
    /**
     * Crea un nuevo usuario en el sistema.
     *
     * Workflow:
     * 1. Validar usuario autenticado
     * 2. Validar autorización (rol)
     * 3. Validar que role existe
     * 4. Validar existencia del ecommerce
     * 5. Validar que email NO existe
     * 6. Validar que username NO existe
     * 7. Validar password
     * 8. Hash password y crear usuario
     * 9. Registrar auditoría
     * 10. Retornar UserResponse
     *
     * @param request datos del usuario a crear
     * @return UserResponse con datos creados
     * @throws UnauthorizedException si usuario no autenticado
     * @throws AuthorizationException si rol insuficiente
     * @throws BadRequestException si validación falla
     * @throws ConflictException si email o username existen
     */
    @Transactional
    @Override
    public UserResponse createUser(UserCreateRequest request) {
        // 1. Validar usuario autenticado
        String currentRole;
        UUID currentUserEcommerceId;
        UUID currentUserUid;
        
        try {
            currentRole = securityContextHelper.getCurrentUserRole();
            currentUserEcommerceId = securityContextHelper.getCurrentUserEcommerceId();
            currentUserUid = securityContextHelper.getCurrentUserUid();
        } catch (Exception e) {
            log.warn("Error obteniendo usuario autenticado: {}", e.getMessage());
            throw new UnauthorizedException("Usuario no autenticado");
        }
        
        if (currentRole == null) {
            log.warn("Usuario autenticado sin rol válido");
            throw new UnauthorizedException("Usuario no autenticado");
        }
        
        // 2. Validar autorización (solo SUPER_ADMIN o STORE_ADMIN)
        if (!"SUPER_ADMIN".equals(currentRole) && !"STORE_ADMIN".equals(currentRole)) {
            log.warn("Intento de crear usuario sin permisos. Role actual: {}", currentRole);
            throw new AuthorizationException("Solo SUPER_ADMIN o STORE_ADMIN pueden crear usuarios");
        }
        
        // 3. Validar que el role existe
        RoleEntity roleEntity = roleRepository.findById(request.roleId())
                .orElseThrow(() -> {
                    log.warn("Intento de crear usuario con roleId inválido: {}", request.roleId());
                    return new BadRequestException("El roleId proporcionado no existe");
                });
        
        String roleName = roleEntity.getName();
        
        // 4. Validar existencia del ecommerce
        if ("SUPER_ADMIN".equals(roleName)) {
            if (request.ecommerceId() != null) {
                log.warn("Intento de crear SUPER_ADMIN con ecommerce_id: {}", request.ecommerceId());
                throw new BadRequestException("SUPER_ADMIN no puede tener ecommerce_id asignado");
            }
        } else {
            if (request.ecommerceId() == null) {
                log.warn("Intento de crear usuario {} sin ecommerce_id", roleName);
                throw new BadRequestException(String.format("%s requiere ecommerce_id obligatorio", roleName));
            }
            
            ecommerceService.validateEcommerceExists(request.ecommerceId());
            
            // Validar que STORE_ADMIN solo crea en su ecommerce
            if ("STORE_ADMIN".equals(currentRole)) {
                if (!currentUserEcommerceId.equals(request.ecommerceId())) {
                    log.warn("STORE_ADMIN intenta crear usuario en otro ecommerce. Own: {}, Requested: {}", 
                            currentUserEcommerceId, request.ecommerceId());
                    throw new AuthorizationException("Solo puede crear usuarios dentro de su ecommerce");
                }
            }
        }
        
        // 5. Validar que email NO existe
        if (userCreatePersistencePort.existsByEmail(request.email())) {
            log.warn("Intento de crear usuario con email duplicado: {}", request.email());
            throw new ConflictException("El email ya existe. Use otro.");
        }
        
        // 6. Validar que username NO existe
        if (userCreatePersistencePort.existsByUsername(request.username())) {
            log.warn("Intento de crear usuario con username duplicado: {}", request.username());
            throw new ConflictException("El username ya existe. Use otro.");
        }
        
        // 7. Validar password
        if (!passwordValidator.isValid(request.password())) {
            String errorMsg = passwordValidator.getErrorMessage(request.password());
            log.warn("Intento de crear usuario con contraseña débil: {}. Username: {}", errorMsg, request.username());
            throw new BadRequestException(errorMsg);
        }
        
        // 8. Hash password y crear usuario
        UserEntity user = UserEntity.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .roleId(roleEntity.getId())
                .role(roleEntity)
                .ecommerceId(request.ecommerceId())
                .isActive(true)
                .build();
        
        UserEntity saved = userCreatePersistencePort.save(user);
        log.info("Usuario creado exitosamente: uid={}, username={}, role={}, ecommerce={}", 
                saved.getId(), saved.getUsername(), saved.getRoleId(), saved.getEcommerceId());
        
        // 9. Registrar auditoría
        auditService.auditUserCreation(saved, currentUserUid);
        
        // 10. Retornar UserResponse
        return toResponse(saved);
    }
    
    private UserResponse toResponse(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRoleId(),
                user.getRole() != null ? user.getRole().getName() : null,
                user.getEmail(),
                user.getEcommerceId(),
                user.getIsActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
