package pl.touk.widerest.multitenancy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.MacSigner;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.sql.SQLException;
import java.util.Optional;

@RestController
@RequestMapping("/tenant")
@Slf4j
@Api(value = "tenants", description = "Tenants registration endpoint")
public class TenantEndpoint {

    @Resource
    private MacSigner signerVerifier;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private MultiTenancyService multiTenancyService;

    @RequestMapping(method = RequestMethod.POST)
    public String create(@ApiParam @RequestBody @Valid TenantRequest tenantRequest, HttpServletRequest request) {

        if (request.getAttribute(MultiTenancyConfig.TENANT_REQUEST_ATTRIBUTE) != null) {
             throw new TenantTokenNotAllowed();
        }

        String tenantIdentifier = RandomStringUtils.randomAlphabetic(16).toLowerCase();
        Tenant tenant = Tenant.builder().id(tenantIdentifier).subscriptionType("free").build();
        request.setAttribute(MultiTenancyConfig.TENANT_REQUEST_ATTRIBUTE, tenant);
        try {
            multiTenancyService.createTenantSchema(tenantIdentifier, Optional.of(tenantRequest));
            return JwtHelper.encode(objectMapper.writeValueAsString(tenant), signerVerifier).getEncoded();
        } catch (JsonProcessingException e) {
            throw new TenantCreationError(e);
        } catch (SQLException e) {
            throw new TenantCreationError(e);
        }
    }

    @RequestMapping(method = RequestMethod.GET)
    public String read(HttpServletRequest request) {

        Tenant tenant = (Tenant) request.getAttribute(MultiTenancyConfig.TENANT_REQUEST_ATTRIBUTE);
        if (tenant == null) {
            throw new TenantTokenMissing();
        }

        try {
            return JwtHelper.encode(objectMapper.writeValueAsString(tenant), signerVerifier).getEncoded();
        } catch (JsonProcessingException e) {
            throw new TenantCreationError(e);
        }
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
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
