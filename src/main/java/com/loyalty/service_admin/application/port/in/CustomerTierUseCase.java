package com.loyalty.service_admin.application.port.in;

import com.loyalty.service_admin.application.dto.customertier.CustomerTierCreateRequest;
import com.loyalty.service_admin.application.dto.customertier.CustomerTierUpdateRequest;
import com.loyalty.service_admin.application.dto.customertier.CustomerTierResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * CustomerTierUseCase - Puerto de entrada para operaciones sobre customer tiers (clasificación de clientes).
 *
 * Abarca:
 * - HU-10: Loyalty Tiers (Customer Classification)
 */
public interface CustomerTierUseCase {

    /**
     * Crear un nuevo customer tier.
     */
    CustomerTierResponse create(CustomerTierCreateRequest request);

    /**
     * Obtener customer tier por ID.
     */
    CustomerTierResponse getById(UUID id);

    /**
     * Listar customer tiers activos, ordenados por nivel jerárquico.
     */
    List<CustomerTierResponse> listActive();

    /**
     * Listar TODOS los customer tiers (incluyendo inactivos).
     */
    List<CustomerTierResponse> listAll();

    /**
     * Eliminar customer tier (soft delete).
     */
    void delete(UUID id);

    /**
     * Listar customer tiers con paginación. Si isActive=true, solo activos; si null, todos.
     */
    Page<CustomerTierResponse> listPaginated(Pageable pageable, Boolean isActive);

    /**
     * Actualizar customer tier.
     */
    CustomerTierResponse update(UUID id, CustomerTierUpdateRequest request);

    /**
     * Activar customer tier.
     */
    CustomerTierResponse activate(UUID id);
}
