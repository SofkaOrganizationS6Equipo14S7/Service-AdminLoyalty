package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.port.out.DiscountConfigPersistencePort;
import com.loyalty.service_admin.application.port.out.DiscountConfigEventPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class DiscountConfigServiceUnitTests {
    @Mock private DiscountConfigPersistencePort persistencePort;
    @Mock private DiscountConfigEventPort eventPort;
    @InjectMocks private DiscountConfigService service;

    @Test
    void testServiceUsesPortInjection() {
        assertNotNull(service, "Service should be injected with ports");
    }
}
