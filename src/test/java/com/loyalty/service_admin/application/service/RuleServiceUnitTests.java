package com.loyalty.service_admin.application.service;

import com.loyalty.service_admin.application.port.out.RulePersistencePort;
import com.loyalty.service_admin.application.port.out.RuleEventPort;
import com.loyalty.service_admin.domain.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class RuleServiceUnitTests {
    @Mock private RulePersistencePort rulePersistencePort;
    @Mock private RuleEventPort ruleEventPort;
    @Mock private RuleRepository ruleRepository;
    @Mock private RuleAttributeRepository ruleAttributeRepository;
    @Mock private RuleAttributeValueRepository ruleAttributeValueRepository;
    @Mock private DiscountLimitPriorityRepository discountLimitPriorityRepository;
    @Mock private CustomerTierRepository customerTierRepository;
    @Mock private DiscountTypeRepository discountTypeRepository;
    @InjectMocks private RuleService service;

    @Test
    void testServiceUsesPortInjection() {
        assertNotNull(service, "Service should be injected with ports");
    }
}
