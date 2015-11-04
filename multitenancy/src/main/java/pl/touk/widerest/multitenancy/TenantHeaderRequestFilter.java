package pl.touk.widerest.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;

@Component
@Slf4j
public class TenantHeaderRequestFilter extends OncePerRequestFilter {

    public static final String TENANT_TOKEN_HEADER = "Tenant-Token";

    @Resource
    private MultiTenancyService multiTenancyService;

    @Resource
    private IdentifierTool identifierTool;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        /* TODO: (mst) remove when all the front-end development is finished */
        final String TEST_HOST = "192.168.60";

        String host = URI.create(request.getRequestURL().toString()).getHost();
        String tenantIdentifier = request.getHeader(TENANT_TOKEN_HEADER);
        if (tenantIdentifier == null && !"localhost".equals(host) && !host.startsWith("127.") && !host.startsWith(TEST_HOST)) {
            tenantIdentifier = host.replaceAll("\\..*", "");
        }
        if (tenantIdentifier != null) {
            try {
                identifierTool.verifyIdentifier(tenantIdentifier);
                if (!MultiTenancyConfig.DEFAULT_TENANT_IDENTIFIER.equals(tenantIdentifier) && !multiTenancyService.checkIfTenantSchemaExists(tenantIdentifier)) {
                    response.sendError(HttpStatus.NOT_FOUND.value(), "Invalid Tenant");
                    return;
                }
            } catch (Exception ex) {
                response.sendError(HttpStatus.NOT_FOUND.value(), "Invalid Tenant");
                return;
            }
            request.setAttribute(MultiTenancyConfig.TENANT_IDENTIFIER_REQUEST_ATTRIBUTE, tenantIdentifier);
        }
        filterChain.doFilter(request, response);
    }

}
