package pl.touk.widerest.multitenancy;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
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
import pl.touk.widerest.security.config.ResourceServerConfig;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(ResourceServerConfig.API_PATH + "/tenant")
@Slf4j
@Api(value = "tenants", description = "Tenants registration endpoint")
public class TenantEndpoint {

    @Resource
    private IdentifierTool identifierTool;

    @Resource
    private ObjectMapper objectMapper;

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

            return tenantIdentifier;
        } catch (Exception e) {
            log.error("Tenant creation error", e);
            throw new TenantCreationError(e);
        }
    }

    @Resource
    private CurrentTenantIdentifierResolver currentTenantIdentifierResolver;

    @RequestMapping(method = RequestMethod.GET)
    public List<String> testRead(
            @ApiIgnore Authentication authentication,
            HttpServletRequest request
    ) {
        if (tenantTokenStore != null) {
            return tenantTokenStore.getTenantTokens(authentication);
        } else {
            String tenantIdentifier = currentTenantIdentifierResolver.resolveCurrentTenantIdentifier();
            identifierTool.verifyIdentifier(tenantIdentifier);
            return Lists.newArrayList(tenantIdentifier);
        }
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
