package com.loyalty.service_admin.infrastructure.persistence.jpa;

import com.loyalty.service_admin.application.port.out.DiscountLimitPriorityPersistencePort;
import com.loyalty.service_admin.domain.entity.DiscountLimitPriorityEntity;
import com.loyalty.service_admin.domain.repository.DiscountLimitPriorityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JpaDiscountLimitPriorityAdapter - Adapter de persistencia para DiscountLimitPriorityService.
 *
 * Implementa DiscountLimitPriorityPersistencePort delegando a DiscountLimitPriorityRepository.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JpaDiscountLimitPriorityAdapter implements DiscountLimitPriorityPersistencePort {

    private final DiscountLimitPriorityRepository repository;

    @Override
    public DiscountLimitPriorityEntity savePriority(DiscountLimitPriorityEntity priority) {
        log.debug("Saving discount limit priority: {}", priority.getId());
        return repository.save(priority);
    }

    @Override
    public Optional<DiscountLimitPriorityEntity> findPriorityById(UUID priorityId) {
        log.debug("Finding discount limit priority by id: {}", priorityId);
        return repository.findById(priorityId);
    }

    @Override
    public List<DiscountLimitPriorityEntity> findPrioritiesByConfig(UUID configId) {
        log.debug("Finding discount limit priorities by config id: {}", configId);
        return repository.findByDiscountSettingsIdOrderByLevelAsc(configId);
    }

    @Override
    public void deletePriority(UUID priorityId) {
        log.debug("Deleting discount limit priority: {}", priorityId);
        repository.deleteById(priorityId);
    }

    @Override
    public boolean existsPriorityWithName(UUID configId, String name) {
        log.debug("Checking if priority exists with name: {} in config: {}", name, configId);
        return repository.existsByDiscountSettingsIdAndName(configId, name);
    }
}
