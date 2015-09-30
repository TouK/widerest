package pl.touk.widerest;

import com.spotify.docker.client.shaded.javax.ws.rs.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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

        //final String allowedHeaders = String.join(", ", httpServletRequest.getHeaders().keySet()) + ", " + requestContext.getHeaderString("Access-Control-Request-Headers");

        final Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
        final Set<String> headerNamesSet = new HashSet<>();

        while(headerNames.hasMoreElements()) {
            headerNamesSet.add(headerNames.nextElement());
        }

        final String allowedHeaders = String.join(", ", headerNamesSet + ", " + "Authorization"/*+ ", " + httpServletRequest.getHeader("Access-Control-Request-Headers")*/);

        httpServletResponse.setHeader("Access-Control-Allow-Origin", allowedOrigin);
        httpServletResponse.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        httpServletResponse.setHeader("Access-Control-Allow-Credentials", "true");
        httpServletResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        httpServletResponse.setHeader("Access-Control-Max-Age", "1209600");

        /* (mst) This is a part of CORS preflight request. There won't
                 be any Authorization headers, therefore to skip Auth0
                 authorization here (which would always return 401) we'll
                 just return a HTTP response with an OK status for each
                 OPTIONS request.
         */
        if(httpServletRequest.getMethod().equals(HttpMethod.OPTIONS)) {
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }


        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }
}
