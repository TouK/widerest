package pl.touk.widerest.base;

import com.google.common.collect.ImmutableList;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

public class MappingHalJackson2HttpMessageConverter extends MappingJackson2HttpMessageConverter {
    public MappingHalJackson2HttpMessageConverter() {
        super();
        this.objectMapper.registerModule(new Jackson2HalModule());
        setSupportedMediaTypes(
                ImmutableList.of(
                        new MediaType("application", "json", DEFAULT_CHARSET),
                        new MediaType("application", "*+json", DEFAULT_CHARSET),
                        MediaTypes.HAL_JSON
         ));
    }
}
