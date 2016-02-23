package pl.touk.widerest.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.JsonViewResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import javaslang.control.Try;
import pl.touk.widerest.security.oauth2.OutOfBandUriHandler;

import java.util.ArrayList;
import java.util.List;


@Configuration
public class OutOfBandUriConfig extends WebMvcConfigurerAdapter {

    @Autowired
    HttpMessageConverters messageConverters;

    @Autowired
    WebMvcConfigurationSupport webMvcConfigurationSupport;

    @Autowired
    ApplicationContext applicationContext;

    @Bean
    HandlerMethodReturnValueHandler oAuth2OutOfBandUriHandler(RequestMappingHandlerAdapter adapter) {
        return new OutOfBandUriHandler(adapter);
    };

    @Override
    public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {

        Try.of(() -> {
            throw new Exception();
        });

        final ExceptionHandlerExceptionResolver exceptionHandlerExceptionResolver = new
                ExceptionHandlerExceptionResolver();
        exceptionHandlerExceptionResolver.setContentNegotiationManager(webMvcConfigurationSupport.mvcContentNegotiationManager());
        exceptionHandlerExceptionResolver.setMessageConverters(this.messageConverters.getConverters());
            List<ResponseBodyAdvice<?>> interceptors = new ArrayList<>();
            interceptors.add(new JsonViewResponseBodyAdvice());
            exceptionHandlerExceptionResolver.setResponseBodyAdvice(interceptors);
        exceptionHandlerExceptionResolver.setApplicationContext(this.applicationContext);
        exceptionHandlerExceptionResolver.afterPropertiesSet();
        new OutOfBandUriHandler(exceptionHandlerExceptionResolver); // TODO to jest celowo ignorowane?
        exceptionResolvers.add(exceptionHandlerExceptionResolver);

        ResponseStatusExceptionResolver responseStatusExceptionResolver = new ResponseStatusExceptionResolver();
        responseStatusExceptionResolver.setMessageSource(this.applicationContext);
        exceptionResolvers.add(responseStatusExceptionResolver);

        exceptionResolvers.add(new DefaultHandlerExceptionResolver());

    }


}
