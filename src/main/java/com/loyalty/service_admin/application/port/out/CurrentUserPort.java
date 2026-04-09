package com.loyalty.service_admin.application.port.out;

import java.util.UUID;

public interface CurrentUserPort {
    boolean isSuperAdmin();

    UUID getCurrentUserEcommerceId();
}
