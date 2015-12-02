package pl.touk.widerest;

import org.broadleafcommerce.openadmin.server.security.domain.AdminUser;
import org.broadleafcommerce.openadmin.server.security.service.AdminSecurityService;
import org.hibernate.Session;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.provider.NoSuchClientException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import pl.touk.widerest.multitenancy.MultiTenancyConfig;
import pl.touk.widerest.multitenancy.TenantRequest;
import pl.touk.widerest.security.jwt.WiderestAccessTokenConverter;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Configuration
@ComponentScan("pl.touk.multitenancy")
public class Multitenancy {

    @ConditionalOnBean(CurrentTenantIdentifierResolver.class)
    @Bean
    Consumer<String> clientIdValidator(CurrentTenantIdentifierResolver currentTenantIdentifierResolver) {
        return clientId -> {
            if (!clientId.startsWith(currentTenantIdentifierResolver.resolveCurrentTenantIdentifier()))
                throw new NoSuchClientException("No client with requested id: " + clientId);
        };
    }

    @ConditionalOnBean(CurrentTenantIdentifierResolver.class)
    @Bean
    public Supplier<String> resourceIdSupplier(CurrentTenantIdentifierResolver currentTenantIdentifierResolver) {
        return currentTenantIdentifierResolver::resolveCurrentTenantIdentifier;
    }

    @ConditionalOnBean(CurrentTenantIdentifierResolver.class)
    @Bean
    public Supplier<String> issuerSupplier(CurrentTenantIdentifierResolver currentTenantIdentifierResolver) {
        return () -> WiderestAccessTokenConverter.WIDEREST_ISS + WiderestAccessTokenConverter.DELIMITER +currentTenantIdentifierResolver.resolveCurrentTenantIdentifier();
    }

    @ConditionalOnBean(CurrentTenantIdentifierResolver.class)
    @Bean
    public Consumer<TenantRequest> setTenantDetails(AdminSecurityService adminSecurityService, CurrentTenantIdentifierResolver tenantIdentifierResolver) {
        return new Consumer<TenantRequest>() {

            @PersistenceContext(unitName = "blPU")
            protected EntityManager em;

            @Override
            public void accept(TenantRequest tenantRequest) {
                Object tenantInRequest = RequestContextHolder.getRequestAttributes().getAttribute(MultiTenancyConfig.TENANT_IDENTIFIER_REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
                String tenantIdentifier = tenantIdentifierResolver.resolveCurrentTenantIdentifier();
                String tenantIdInEntityManager = em.unwrap(Session.class).getTenantIdentifier();
                AdminUser adminUser = adminSecurityService.readAdminUserByUserName("admin");
                adminUser.setEmail(tenantRequest.getAdminEmail());
                adminUser.setUnencodedPassword(tenantRequest.getAdminPassword());
                adminSecurityService.saveAdminUser(adminUser);
            }
        };
    }
}
