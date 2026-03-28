package com.loyalty.service_admin.infrastructure.adapter;

import com.loyalty.service_admin.application.port.out.CurrentUserPort;
import com.loyalty.service_admin.infrastructure.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CurrentUserSecurityAdapter implements CurrentUserPort {

    private final SecurityContextHelper securityContextHelper;

    @Override
    public boolean isSuperAdmin() {
        return securityContextHelper.isCurrentUserSuperAdmin();
    }

    @Override
    public UUID getCurrentUserEcommerceId() {
        return securityContextHelper.getCurrentUserEcommerceId();
    }
}
