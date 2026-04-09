package com.loyalty.service_admin.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validador de contraseñas para el sistema LOYALTY.
 * SPEC-004 RN-06: Contraseña mínimo 12 caracteres con mayúscula, minúscula y número.
 * 
 * Responsabilidades:
 * - Validar complejidad de contraseña
 * - Verificar requisitos mínimos de seguridad
 * - Proporcionar mensajes de error descriptivos
 */
@Component
@Slf4j
public class PasswordValidator {
    
    public static final int MIN_LENGTH = 12;
    public static final String PASSWORD_PATTERN = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{" + MIN_LENGTH + ",}$";
    
    /**
     * Valida una contraseña según los requisitos de SPEC-004 RN-06:
     * - Mínimo 12 caracteres
     * - Al menos una mayúscula
     * - Al menos una minúscula
     * - Al menos un número
     * 
     * @param password la contraseña a validar
     * @return true si cumple todos los requisitos, false en caso contrario
     */
    public boolean isValid(String password) {
        return password != null && password.matches(PASSWORD_PATTERN);
    }
    
    /**
     * Retorna un mensaje de error descriptivo si la contraseña no es válida.
     * 
     * @param password la contraseña a validar
     * @return mensaje de error, o null si la contraseña es válida
     */
    public String getErrorMessage(String password) {
        if (password == null || password.isEmpty()) {
            return "La contraseña no puede estar vacía";
        }
        
        if (password.length() < MIN_LENGTH) {
            return String.format("La contraseña debe tener mínimo %d caracteres (actual: %d)",
                    MIN_LENGTH, password.length());
        }
        
        if (!password.matches(".*[A-Z].*")) {
            return "La contraseña debe contener al menos una mayúscula (A-Z)";
        }
        
        if (!password.matches(".*[a-z].*")) {
            return "La contraseña debe contener al menos una minúscula (a-z)";
        }
        
        if (!password.matches(".*\\d.*")) {
            return "La contraseña debe contener al menos un número (0-9)";
        }
        
        return null;
    }
}
