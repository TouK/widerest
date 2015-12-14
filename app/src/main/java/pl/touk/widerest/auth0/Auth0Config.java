package pl.touk.widerest.auth0;

import com.auth0.spring.security.auth0.Auth0AuthenticationEntryPoint;
import com.auth0.spring.security.auth0.Auth0AuthenticationProvider;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import pl.touk.multitenancy.MultiTenancyConfig;
import pl.touk.multitenancy.TenantTokenStore;
import springfox.documentation.service.Documentation;
import springfox.documentation.spring.web.DocumentationCache;
import springfox.documentation.swagger.web.ApiKeyVehicle;
import springfox.documentation.swagger.web.SecurityConfiguration;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ConditionalOnClass({TenantTokenStore.class})
@ConditionalOnBean(CurrentTenantIdentifierResolver.class)
@ConditionalOnProperty("auth0.domain")
@Configuration
@ComponentScan("com.auth0")
@Order(99)
public class Auth0Config extends WebSecurityConfigurerAdapter {

    @Value("${auth0.clientSecret}")
    public String clientSecret;

    @Value("${auth0.clientId}")
    public String clientId;

    @Value("${auth0.securedRoute:/tenant}")
    public String securedRoot;

    @Bean
    public Auth0TenantTokenStore auth0TenantTokenStore() {
        return new Auth0TenantTokenStore();
    }

    @Bean
    public Auth0AuthenticationProvider auth0AuthenticationProvider() {
        Auth0AuthenticationProvider provider = new Auth0AuthenticationProvider();
        provider.setClientSecret(clientSecret);
        provider.setClientId(clientId);
        provider.setSecuredRoute(securedRoot);
        return provider;
    }


    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(auth0AuthenticationProvider());
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        Auth0AuthenticationEntryPoint authenticationEntryPoint = new Auth0AuthenticationEntryPoint();
        Auth0AuthenticationFilter authenticationFilter = new Auth0AuthenticationFilter();
        authenticationFilter.setAuthenticationManager(authenticationManager());
        authenticationFilter.setEntryPoint(authenticationEntryPoint);

        http
            .addFilterAfter(authenticationFilter, SecurityContextPersistenceFilter.class)
            .exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPoint)
                .and()
            .requestMatchers()
                .antMatchers(securedRoot)
                .and()
            .authorizeRequests()
                .anyRequest()
                .authenticated()
                .and()
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        ;

    }

    private @Autowired
    CurrentTenantIdentifierResolver currentTenantIdentifierResolver;

    @Bean
    public SecurityConfiguration security() {
        return new SecurityConfiguration(
                null,
                "secret",
                "test-app-realm",
                "test-app",
                null,
                ApiKeyVehicle.HEADER,
                " "
        ) {
            @Override
            public String getClientId() {
                return Optional.ofNullable((String)RequestContextHolder.getRequestAttributes().getAttribute(MultiTenancyConfig.TENANT_IDENTIFIER_REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST))
                        .map(tenantId -> tenantId + ".swagger")
                        .orElse(Auth0Config.this.clientId);
            }
        };
    }

    @Primary
    @Bean
    public DocumentationCache resourceGroupCacheOverride() {
        return new DocumentationCache() {

            @Override
            public Map<String, Documentation> all() {
                String tenantIdentifier = Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                        .map(attributes -> attributes.getAttribute(MultiTenancyConfig.TENANT_IDENTIFIER_REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST))
                        .map(String.class::cast)
                        .orElse(null);
                Map<String, Documentation> all = super.all().entrySet().stream()
                        .filter(entry ->
                                        "api".equals(entry.getKey()) != /*XOR*/ (tenantIdentifier == null)
                        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                return all;
            }
        };
    }


}
