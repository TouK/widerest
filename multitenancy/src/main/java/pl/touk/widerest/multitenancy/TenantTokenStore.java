package pl.touk.widerest.multitenancy;

import org.springframework.security.core.Authentication;

import java.util.List;

public interface TenantTokenStore {
    void addTenantToken(String tenantToken, Authentication authentication);
    List<String> getTenantTokens(Authentication authentication);
}
