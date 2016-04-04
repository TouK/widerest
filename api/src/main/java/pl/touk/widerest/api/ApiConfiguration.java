package pl.touk.widerest.api;

import cz.jirutka.spring.exhandler.RestHandlerExceptionResolver;
import cz.jirutka.spring.exhandler.handlers.ErrorMessageRestExceptionHandler;
import cz.jirutka.spring.exhandler.messages.ErrorMessage;
import org.broadleafcommerce.common.web.BroadleafRequestInterceptor;
import org.broadleafcommerce.core.checkout.service.exception.CheckoutException;
import org.broadleafcommerce.core.order.service.exception.UpdateCartException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RelProvider;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@Configuration
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
public class ApiConfiguration extends WebMvcConfigurerAdapter {

    @Bean
    public BroadleafRequestInterceptor broadleafRequestInterceptor() {
        return new BroadleafRequestInterceptor();
    }

    @Bean
    public ChannelInterceptor channelInterceptor() {
        return new ChannelInterceptor();

    }

    @Bean
    public RelProvider relProvider() {
        return new JsonRootRelProvider();
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaTypes.HAL_JSON);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addWebRequestInterceptor(broadleafRequestInterceptor());
        registry.addWebRequestInterceptor(channelInterceptor());
    }

    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        return new MethodValidationPostProcessor();
    }

    @Bean
    public RestHandlerExceptionResolver restExceptionResolver() {
        return RestHandlerExceptionResolver.builder()
//                .messageSource( httpErrorMessageSource() )
                .defaultContentType(MediaType.APPLICATION_JSON)
                .addHandler(CheckoutException.class, new ErrorMessageRestExceptionHandler<CheckoutException>(CheckoutException.class, HttpStatus.CONFLICT) {
                    @Override
                    public ErrorMessage createBody(CheckoutException ex, HttpServletRequest req) {
                        ErrorMessage errorMessage = super.createBody(ex, req);
                        errorMessage.setDetail(ex.getMessage());
                        return errorMessage;
                    }
                })
                .addHandler(Exception.class, new ErrorMessageRestExceptionHandler(Exception.class, HttpStatus.INTERNAL_SERVER_ERROR) {
                    @Override
                    public ResponseEntity handleException(Exception ex, HttpServletRequest req) {
                        logException(ex, req);

                        ErrorMessage body = createBody(ex, req);
                        HttpHeaders headers = createHeaders(ex, req);

                        HttpStatus status = Optional.ofNullable(AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class))
                                .map(ResponseStatus::value).orElse(getStatus());
                        body.setStatus(status);
                        if (StringUtils.isEmpty(body.getTitle())) body.setTitle(status.getReasonPhrase());

                        return new ResponseEntity<>(body, headers, status);
                    }

                })
                .addErrorMessageHandler(UpdateCartException.class, HttpStatus.CONFLICT)
                .build();
    }


}
