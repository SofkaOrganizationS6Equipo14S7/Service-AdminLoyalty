package com.loyalty.service_admin.application.port.in;

import com.loyalty.service_admin.application.dto.ecommerce.EcommerceCreateRequest;
import com.loyalty.service_admin.application.dto.ecommerce.EcommerceResponse;
import com.loyalty.service_admin.application.dto.ecommerce.EcommerceUpdateStatusRequest;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface EcommerceUseCase {
    
    /**
     * Crea un nuevo ecommerce.
     * 
     * @param request con name y slug
     * @return respuesta con uid, name, slug, status, timestamps
     * @throws ConflictException si slug ya existe
     * @throws BadRequestException si datos inválidos
     * @throws RuntimeException si falla publicación de evento
     */
    EcommerceResponse createEcommerce(EcommerceCreateRequest request);
    
    /**
     * Lista ecommerces con filtro opcional de status y paginación.
     * 
     * @param status filtro opcional (ACTIVE/INACTIVE); null = todos
     * @param page número de página (0-indexed)
     * @param size tamaño de la página
     * @return página de respuestas con ecommerces
     * @throws BadRequestException si status inválido
     */
    Page<EcommerceResponse> listEcommerces(String status, int page, int size);
    
    /**
     * Obtiene un ecommerce por uid.
     * 
     * @param uid identificador único
     * @return respuesta con ecommerce
     * @throws EcommerceNotFoundException si no existe
     */
    EcommerceResponse getEcommerceById(UUID uid);
    
    /**
     * Actualiza el status de un ecommerce.
     * 
     * @param uid identificador único
     * @param request con nuevo status
     * @return respuesta con ecommerce actualizado
     * @throws EcommerceNotFoundException si no existe
     * @throws BadRequestException si status inválido
     * @throws RuntimeException si falla publicación de evento
     */
    EcommerceResponse updateEcommerceStatus(UUID uid, EcommerceUpdateStatusRequest request);
    
    /**
     * Valida que un ecommerce existe.
     * 
     * @param ecommerceId uuid a validar
     * @throws EcommerceNotFoundException si no existe o es null
     */
    void validateEcommerceExists(UUID ecommerceId);
}
