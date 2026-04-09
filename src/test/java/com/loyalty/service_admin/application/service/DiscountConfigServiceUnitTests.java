package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.port.out.DiscountConfigPersistencePort;
import com.loyalty.service_admin.application.port.out.DiscountConfigEventPort;
import com.loyalty.service_admin.domain.repository.DiscountLimitPriorityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Placeholder retained for backwards compatibility.
 * Comprehensive tests are in DiscountConfigServiceTest.java.
 */
@ExtendWith(MockitoExtension.class)
class DiscountConfigServiceUnitTests {
    @Mock private DiscountConfigPersistencePort persistencePort;
    @Mock private DiscountConfigEventPort eventPort;
    @Mock private DiscountLimitPriorityRepository priorityRepository;
    @InjectMocks private DiscountConfigService service;

    @Test
    void testServiceUsesPortInjection() {
        assertNotNull(service, "Service should be injected with ports");
    }
}
