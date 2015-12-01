package pl.touk.widerest.multitenancy;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

import pl.touk.widerest.security.config.ResourceServerConfig;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/tenant")
@Slf4j
@Api(value = "tenants", description = "Tenants registration endpoint")
public class TenantEndpoint {

    @Resource
    private IdentifierTool identifierTool;

    @Resource
    private MultiTenancyService multiTenancyService;

    @Autowired(required = false)
    private TenantTokenStore tenantTokenStore;

    @RequestMapping(method = RequestMethod.POST)
    public String create(@ApiParam @RequestBody @Valid TenantRequest tenantRequest,
                         @ApiIgnore Authentication authentication,
                         HttpServletRequest request) {

        String tenantIdentifier = identifierTool.generateIdentifier();
        RequestContextHolder.getRequestAttributes().setAttribute(MultiTenancyConfig.TENANT_IDENTIFIER_REQUEST_ATTRIBUTE, tenantIdentifier, RequestAttributes.SCOPE_REQUEST);
        try {
            multiTenancyService.createTenantSchema(tenantIdentifier, Optional.of(tenantRequest));

            if (tenantTokenStore != null) {
                tenantTokenStore.addTenantToken(tenantIdentifier, authentication);
            }

            if ("localhost".equals(request.getServerName())) {
                return tenantIdentifier;
            } else {
                final String domain = getDomain(request);
                return Optional.of(tenantIdentifier)
                        .map(i -> ServletUriComponentsBuilder.fromContextPath(request).host(i + "." + domain).build())
                        .map(UriComponents::toUriString)
                        .get();
            }
        } catch (Exception e) {
            log.error("Tenant creation error", e);
            throw new TenantCreationError(e);
        }
    }

    @Resource
    private CurrentTenantIdentifierResolver currentTenantIdentifierResolver;

    @RequestMapping(method = RequestMethod.GET)
    public Collection<String> readAll(
            @ApiIgnore Authentication authentication,
            HttpServletRequest request
    ) {
        Stream<String> tenantIdentifiers;
        if (tenantTokenStore != null) {
            tenantIdentifiers = tenantTokenStore.getTenantTokens(authentication).stream();

        } else {
            String tenantIdentifier = currentTenantIdentifierResolver.resolveCurrentTenantIdentifier();
            identifierTool.verifyIdentifier(tenantIdentifier);
            tenantIdentifiers = Stream.of(tenantIdentifier);
        }

        tenantIdentifiers = tenantIdentifiers.filter(multiTenancyService::checkIfTenantSchemaExists);

        final String domain = getDomain(request);

        if (!"localhost".equals(request.getServerName())) {
            tenantIdentifiers = tenantIdentifiers
                    .map(i -> ServletUriComponentsBuilder.fromContextPath(request).host(i + "." + domain).build())
                    .map(UriComponents::toUriString);
        }

        List<String> tenantIdentifiersList = tenantIdentifiers.collect(Collectors.toList());
        return tenantIdentifiersList;
    }

    private String getDomain(HttpServletRequest request) {
        String[] domainParts = request.getServerName().split("\\.");
        return String.join(".", Arrays.copyOfRange(domainParts, domainParts.length > 2 ? 1 : 0, domainParts.length));
    }

    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    static class TenantTokenMissing extends RuntimeException {}

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    static class TenantTokenNotAllowed extends RuntimeException {}

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    static class TenantCreationError extends RuntimeException {
        public TenantCreationError(Throwable cause) {
            super(cause);
        }
    }

}
