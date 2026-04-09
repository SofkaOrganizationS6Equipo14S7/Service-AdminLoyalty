package com.loyalty.service_admin.infrastructure.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * Custom Principal que almacena ecommerce_id para aislamiento multi-tenant.
 * 
 * Responsabilidades:
 * - Almacenar uid del usuario (UUID)
 * - Almacenar ecommerce_id para filtrado automático (multi-tenant isolation)
 * - Implementar UserDetails de Spring Security
 * 
 * Implementa SPEC-002 punto 6: Contexto de seguridad con Context Holder
 * 
 * Uso:
 * UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
 *     .getAuthentication().getPrincipal();
 * UUID ecommerceId = principal.getEcommerceId();
 */
public class UserPrincipal implements UserDetails {
    
    private final UUID uid;
    private final String username;
    private final String password;
    private final String role;
    private final UUID ecommerceId; // null si es SUPER_ADMIN
    private final boolean enabled;
    
    public UserPrincipal(
        UUID uid,
        String username,
        String password,
        String role,
        UUID ecommerceId,
        boolean enabled
    ) {
        this.uid = uid;
        this.username = username;
        this.password = password;
        this.role = role;
        this.ecommerceId = ecommerceId;
        this.enabled = enabled;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }
    
    @Override
    public String getPassword() {
        return password;
    }
    
    @Override
    public String getUsername() {
        return username;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    // Getters personalizados
    public UUID getUid() {
        return uid;
    }
    
    /**
     * Retorna el ecommerce_id del usuario.
     * es null si el usuario es SUPER_ADMIN (sin restricción de ecommerce).
     */
    public UUID getEcommerceId() {
        return ecommerceId;
    }
    
    public String getRole() {
        return role;
    }
    
    /**
     * Verdadero si el usuario tiene restricción de ecommerce (no es super admin).
     */
    public boolean isEcommerceScoped() {
        return ecommerceId != null;
    }
}
