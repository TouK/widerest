package pl.touk.widerest.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.broadleafcommerce.common.web.BroadleafRequestInterceptor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.hateoas.RelProvider;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.hal.CurieProvider;
import org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import pl.touk.widerest.MultiLinkAwareJackson2HalModule;

import org.springframework.hateoas.MediaTypes;

import javax.annotation.Resource;
import javax.persistence.EntityManagerFactory;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableTransactionManagement
public class ApiConfiguration extends WebMvcConfigurerAdapter implements TransactionManagementConfigurer {

    private static final String DELEGATING_REL_PROVIDER_BEAN_NAME = "_relProvider";
    private static final String LINK_DISCOVERER_REGISTRY_BEAN_NAME = "_linkDiscovererRegistry";
    private static final String HAL_OBJECT_MAPPER_BEAN_NAME = "_halObjectMapper";


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

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addWebRequestInterceptor(broadleafRequestInterceptor());
    }


    /* (mst) HAL stuff for enabling links to be rendered as an array. Not ready, yet. */

//    @Autowired
//    private ListableBeanFactory beanFactory;
//
//
//    @Override
//    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
//
//        CurieProvider curieProvider = getCurieProvider(beanFactory);
//        RelProvider relProvider = beanFactory.getBean(DELEGATING_REL_PROVIDER_BEAN_NAME, RelProvider.class);
//        ObjectMapper halObjectMapper = beanFactory.getBean(HAL_OBJECT_MAPPER_BEAN_NAME, ObjectMapper.class);
//
//        halObjectMapper.registerModule(new MultiLinkAwareJackson2HalModule());
//        halObjectMapper.setHandlerInstantiator(new MultiLinkAwareJackson2HalModule.MultiLinkAwareHalHandlerInstantiator(relProvider, curieProvider));
//
//        MappingJackson2HttpMessageConverter halConverter = new TypeConstrainedMappingJackson2HttpMessageConverter(ResourceSupport.class);
//        halConverter.setSupportedMediaTypes(Arrays.asList(MediaTypes.HAL_JSON, MediaType.APPLICATION_JSON));
//        halConverter.setObjectMapper(halObjectMapper);
//
//        converters.add(halConverter);
//    }
//
//
//    private static CurieProvider getCurieProvider(BeanFactory factory) {
//        try {
//            return factory.getBean(CurieProvider.class);
//        } catch (NoSuchBeanDefinitionException e) {
//            return null;
//        }
//    }


}
