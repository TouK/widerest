package pl.touk.widerest;

import java.util.Iterator;
import java.util.List;

import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;

@Component
public class ReorderedHttpMessageConverters extends HttpMessageConverters {
    @Override
    protected List<HttpMessageConverter<?>> postProcessConverters(List<HttpMessageConverter<?>> converters) {
        for (Iterator<HttpMessageConverter<?>> iterator = converters.iterator(); iterator.hasNext();) {
            HttpMessageConverter<?> converter = iterator.next();
            if (converter instanceof StringHttpMessageConverter) {
                iterator.remove();
                converters.add(converter);
                break;
            }
        }
        return converters;
    }
}
