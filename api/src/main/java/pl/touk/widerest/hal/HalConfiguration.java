package pl.touk.widerest.hal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.RelProvider;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import pl.touk.widerest.api.JsonRootRelProvider;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

@Configuration
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
public class HalConfiguration {

    @Autowired(required = false)
    Collection<RequestMappingHandlerAdapter> requestMappingHandlerAdapters = Collections.emptyList();

    @Autowired(required = false)
    Collection<AnnotationMethodHandlerAdapter> annotationMethodHandlerAdapters = Collections.emptyList();

    @Autowired(required = false)
    Collection<RestTemplate> restTemplates = Collections.emptyList();

    @PostConstruct
    public void addEmbeddedResourcesMixinsToHalObjectMappers() {

        Stream.concat(Stream.concat(
                requestMappingHandlerAdapters.stream()
                        .map(RequestMappingHandlerAdapter::getMessageConverters)
                        .flatMap(Collection::stream),
                annotationMethodHandlerAdapters.stream()
                        .map(AnnotationMethodHandlerAdapter::getMessageConverters)
                        .flatMap(Arrays::stream)),
                restTemplates.stream()
                        .map(RestTemplate::getMessageConverters)
                        .flatMap(Collection::stream)

        )
                .filter(MappingJackson2HttpMessageConverter.class::isInstance)
                .map(MappingJackson2HttpMessageConverter.class::cast)
                .map(MappingJackson2HttpMessageConverter::getObjectMapper)
                .filter(Jackson2HalModule::isAlreadyRegisteredIn)
                .distinct()
                .forEach(objectMapper -> {
                    objectMapper.addMixIn(ResourceSupportWithEmbedded.class, ResourceSupportWithEmbeddedMixIn.class);
                });
        ;

        requestMappingHandlerAdapters.getClass();

    }

    @Bean
    public RelProvider relProvider() {
        return new JsonRootRelProvider();
    }



}
