package org.broadleafcommerce.ext;

import org.broadleafcommerce.core.web.order.security.CartStateRequestProcessor;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component("blBiddingsStateFilter")
public class BiddingsStateFilter  extends OncePerRequestFilter implements Ordered {

    @Resource(name = "blBiddingsStateRequestProcessor")
    protected BiddingsStateRequestProcessor biddingsStateProcessor;

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        biddingsStateProcessor.process(new ServletWebRequest(request, response));
        chain.doFilter(request, response);
    }

    @Override
    public int getOrder() {
        return 1502;
    }
}
