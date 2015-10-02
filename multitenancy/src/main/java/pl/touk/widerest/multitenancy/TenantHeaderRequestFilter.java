package pl.touk.widerest.multitenancy;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.MacSigner;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Slf4j
public class TenantHeaderRequestFilter extends OncePerRequestFilter {

    public static final String TENANT_TOKEN_HEADER = "Tenant-Token";

    @Resource
    private MacSigner signerVerifier;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private MultiTenancyService multiTenancyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String tenantToken = request.getHeader(TENANT_TOKEN_HEADER);
        if (tenantToken != null) {
            Jwt jwt = null;
            try {
                jwt = JwtHelper.decodeAndVerify(tenantToken, signerVerifier);
            } catch (Exception ex) {
                throw new InvalidTenantToken();
            }
            Tenant tenant = objectMapper.readValue(jwt.getClaims(), Tenant.class);
            if (!multiTenancyService.checkIfTenantSchemaExists(tenant.getId())) {
                throw new InvalidTenantToken();
            }

            request.setAttribute(MultiTenancyConfig.TENANT_REQUEST_ATTRIBUTE, tenant);
        }
        filterChain.doFilter(request, response);
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    static class InvalidTenantToken extends RuntimeException {}

}
