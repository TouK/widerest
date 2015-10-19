package pl.touk.widerest.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.annotation.Resource;
import java.util.Optional;

@Slf4j
@Service
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver {

    @Resource
    MultiTenancyService multiTenancyService;

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenantIdentifier = Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .map(attributes -> attributes.getAttribute(MultiTenancyConfig.TENANT_IDENTIFIER_REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST))
                .map(String.class::cast)
                .orElse(MultiTenancyConfig.DEFAULT_TENANT_IDENTIFIER);
        return tenantIdentifier;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
