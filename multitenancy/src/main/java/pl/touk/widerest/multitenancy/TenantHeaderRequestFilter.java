package pl.touk.widerest.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class TenantHeaderRequestFilter extends OncePerRequestFilter {

    public static final String TENANT_TOKEN_HEADER = "Tenant-Token";

    @Value("#{'${multitenancy.manager.hostnames:manage}'.split(',')}")
    private List<String> managingHostnames;

    @Resource
    private MultiTenancyService multiTenancyService;

    @Resource
    private IdentifierTool identifierTool;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String tenantIdentifier = resolveTenantIdentifier(request);

        if (tenantIdentifier != null) {
            try {
                identifierTool.verifyIdentifier(tenantIdentifier);
                if (!MultiTenancyConfig.DEFAULT_TENANT_IDENTIFIER.equals(tenantIdentifier) && !multiTenancyService.checkIfTenantSchemaExists(tenantIdentifier)) {
                    response.sendError(HttpStatus.NOT_FOUND.value(), "Invalid Tenant: " + tenantIdentifier);
                    return;
                }
            } catch (Exception ex) {
                response.sendError(HttpStatus.NOT_FOUND.value(), "Invalid Tenant: " + tenantIdentifier);
                return;
            }
            request.setAttribute(MultiTenancyConfig.TENANT_IDENTIFIER_REQUEST_ATTRIBUTE, tenantIdentifier);
        }
        filterChain.doFilter(request, response);
    }

    protected String resolveTenantIdentifier(HttpServletRequest request) {
        String host = request.getServerName();
        String tenantIdentifier = request.getHeader(TENANT_TOKEN_HEADER);
        String[] domainParts = host.split("\\.");
        if (tenantIdentifier == null) {
            if (domainParts.length < 3) {
                tenantIdentifier = MultiTenancyConfig.DEFAULT_TENANT_IDENTIFIER;
            } else if (!managingHostnames.contains(domainParts[0])) {
                tenantIdentifier = domainParts[0];
            }
        }
        return tenantIdentifier;
    }

}
