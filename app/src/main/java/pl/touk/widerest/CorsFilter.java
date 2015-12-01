package pl.touk.widerest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

/**
 * Created by mst on 14.09.15.
 */
@Slf4j
@Component
public class CorsFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {

        final String allowedOrigin = Optional.ofNullable(httpServletRequest.getHeader("origin"))
                .orElse(Optional.ofNullable(httpServletRequest.getHeader("referer"))
                    .map(referer -> URI.create(referer).getHost())
                    .orElse("*")
                );

        httpServletResponse.setHeader("Access-Control-Allow-Origin", allowedOrigin);

        httpServletResponse.setHeader("Access-Control-Expose-Headers", "Location");

        httpServletResponse.setHeader("Access-Control-Allow-Credentials", "true");

        if(HttpMethod.OPTIONS.equals(httpServletRequest.getMethod())) {

            httpServletResponse.setHeader("Access-Control-Max-Age", "1209600");

            httpServletResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");

            httpServletResponse.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Tenant-Token");

            /* (mst) This is a part of CORS preflight request. There won't
                     be any Authorization headers, therefore to skip Auth0
                     authorization here (which would always return 401) we'll
                     just return a HTTP response with an OK status for each
                     OPTIONS request.
             */
            httpServletResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }


        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }
}
