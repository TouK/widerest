package pl.touk.widerest.api;

import cz.jirutka.spring.exhandler.RestHandlerExceptionResolver;
import cz.jirutka.spring.exhandler.handlers.ErrorMessageRestExceptionHandler;
import cz.jirutka.spring.exhandler.messages.ErrorMessage;
import org.broadleafcommerce.common.web.BroadleafRequestInterceptor;
import org.broadleafcommerce.core.order.service.exception.UpdateCartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RelProvider;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.annotation.Resource;
import javax.persistence.EntityManagerFactory;
import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@Configuration
@EnableTransactionManagement
@EnableSpringDataWebSupport
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
public class ApiConfiguration extends WebMvcConfigurerAdapter implements TransactionManagementConfigurer {

    @Autowired
    PlatformTransactionManager blTransactionManager;

    @Override
    public PlatformTransactionManager annotationDrivenTransactionManager() {
        return blTransactionManager;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        propertySourcesPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(true);
        return propertySourcesPlaceholderConfigurer;
    }

    @Resource(name = "entityManagerFactory")
    private EntityManagerFactory entityManagerFactory;

    @Primary
    @Bean
    public EntityManagerFactory primaryEntityManagerFactory() {
        return entityManagerFactory;
    }

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
    public RestHandlerExceptionResolver restExceptionResolver() {
        return RestHandlerExceptionResolver.builder()
//                .messageSource( httpErrorMessageSource() )
                .defaultContentType(MediaType.APPLICATION_JSON)
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
