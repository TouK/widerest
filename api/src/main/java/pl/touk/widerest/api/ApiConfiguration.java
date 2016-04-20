package pl.touk.widerest.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import cz.jirutka.spring.exhandler.RestHandlerExceptionResolver;
import org.broadleafcommerce.common.web.BroadleafRequestInterceptor;
import org.broadleafcommerce.core.checkout.service.exception.CheckoutException;
import org.broadleafcommerce.core.order.service.exception.UpdateCartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import pl.touk.widerest.hal.HalConfiguration;

import javax.annotation.PostConstruct;
import java.util.Collection;

@Configuration
@Import(HalConfiguration.class)
public class ApiConfiguration extends WebMvcConfigurerAdapter {

    @Autowired
    Collection<ObjectMapper> objectMappers;

    @PostConstruct
    public void initJodaTimeJacksonModule() {
        objectMappers.forEach(objectMapper -> {
            objectMapper.registerModule(new JSR310Module());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        });
    }

    @Bean
    public BroadleafRequestInterceptor broadleafRequestInterceptor() {
        return new BroadleafRequestInterceptor();
    }

    @Bean
    public ChannelInterceptor channelInterceptor() {
        return new ChannelInterceptor();

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
                .addHandler(CheckoutException.class, new ErrorMessageRestExceptionHandlerWithDefaults(CheckoutException.class, HttpStatus.CONFLICT))
                .addHandler(Exception.class, new ErrorMessageRestExceptionHandlerWithDefaults(Exception.class, HttpStatus.INTERNAL_SERVER_ERROR))
                .addErrorMessageHandler(UpdateCartException.class, HttpStatus.CONFLICT)
                .build();
    }


}
