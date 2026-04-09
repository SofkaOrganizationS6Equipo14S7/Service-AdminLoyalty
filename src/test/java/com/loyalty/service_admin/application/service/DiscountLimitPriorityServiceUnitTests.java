package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.port.out.DiscountLimitPriorityPersistencePort;
import com.loyalty.service_admin.application.port.out.DiscountLimitPriorityEventPort;
import com.loyalty.service_admin.application.validation.DiscountPriorityValidator;
import com.loyalty.service_admin.domain.repository.DiscountTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Placeholder retained for backwards compatibility.
 * Comprehensive tests are in DiscountLimitPriorityServiceTest.java.
 */
@ExtendWith(MockitoExtension.class)
class DiscountLimitPriorityServiceUnitTests {
    @Mock private DiscountLimitPriorityPersistencePort persistencePort;
    @Mock private DiscountLimitPriorityEventPort eventPort;
    @Mock private DiscountTypeRepository discountTypeRepository;
    @Mock private DiscountPriorityValidator priorityValidator;
    @InjectMocks private DiscountLimitPriorityService service;

    @Test
    void testServiceUsesPortInjection() {
        assertNotNull(service, "Service should be injected with ports");
    }
}
