package pl.touk.widerest.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.MacSigner;
import org.springframework.stereotype.Component;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String tenantToken = request.getHeader(TENANT_TOKEN_HEADER);
        if (tenantToken != null) {
            JwtHelper.decodeAndVerify(tenantToken, signerVerifier);
            request.setAttribute(CurrentTenantIdentifierResolverImpl.TENANT_TOKEN_ATTRIBUTE, tenantToken);
        }
        filterChain.doFilter(request, response);
    }
}
