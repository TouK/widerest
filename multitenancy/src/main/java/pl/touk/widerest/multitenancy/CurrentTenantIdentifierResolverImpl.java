package pl.touk.widerest.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Optional;

@Slf4j
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver {

    public static final String TENANT_TOKEN_ATTRIBUTE = "TENANT_TOKEN";

    @Override
    public String resolveCurrentTenantIdentifier() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .map(attributes -> attributes.getAttribute(TENANT_TOKEN_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST))
                .map(Object::toString)
                .map(JwtHelper::decode)
                .map(Jwt::getClaims)
                .orElse(RandomStringUtils.randomAlphabetic(16))
        ;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
