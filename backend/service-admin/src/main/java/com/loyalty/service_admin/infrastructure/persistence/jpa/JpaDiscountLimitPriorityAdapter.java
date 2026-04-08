package com.loyalty.service_admin.infrastructure.persistence.jpa;

import com.loyalty.service_admin.application.port.out.DiscountLimitPriorityPersistencePort;
import com.loyalty.service_admin.domain.entity.DiscountPriorityEntity;
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
    public DiscountPriorityEntity savePriority(DiscountPriorityEntity priority) {
        log.debug("Saving discount limit priority: {}", priority.getId());
        return repository.save(priority);
    }

    @Override
    public Optional<DiscountPriorityEntity> findPriorityById(UUID priorityId) {
        log.debug("Finding discount limit priority by id: {}", priorityId);
        return repository.findById(priorityId);
    }

    @Override
    public List<DiscountPriorityEntity> findPrioritiesByConfig(UUID configId) {
        log.debug("Finding discount limit priorities by config id: {}", configId);
        return repository.findByDiscountSettingsIdOrderByPriorityLevel(configId);
    }

    @Override
    public void deletePriority(UUID priorityId) {
        log.debug("Deleting discount limit priority: {}", priorityId);
        repository.deleteById(priorityId);
    }

    @Override
    public boolean existsPriorityWithLevel(UUID configId, Integer priorityLevel) {
        log.debug("Checking if priority exists with level: {} in config: {}", priorityLevel, configId);
        return repository.existsByDiscountSettingsIdAndPriorityLevel(configId, priorityLevel);
    }
}
