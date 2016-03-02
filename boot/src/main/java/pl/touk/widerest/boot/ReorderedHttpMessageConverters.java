package pl.touk.widerest.boot;

import com.google.common.collect.Lists;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ReorderedHttpMessageConverters extends HttpMessageConverters {

    public ReorderedHttpMessageConverters(HttpMessageConverter<?>... additionalConverters) {
        super(additionalConverters);
    }

    public ReorderedHttpMessageConverters(Collection<HttpMessageConverter<?>> additionalConverters) {
        super(additionalConverters);
    }

    public ReorderedHttpMessageConverters(boolean addDefaultConverters, Collection<HttpMessageConverter<?>> converters) {
        super(addDefaultConverters, converters);
    }

    @Override
    protected List<HttpMessageConverter<?>> postProcessConverters(List<HttpMessageConverter<?>> converters) {
        List<HttpMessageConverter<?>> tail = Lists.newLinkedList();

        for (Iterator<HttpMessageConverter<?>> iterator = converters.iterator(); iterator.hasNext();) {
            HttpMessageConverter<?> converter = iterator.next();
            if (converter instanceof StringHttpMessageConverter) {
                iterator.remove();
                tail.add(converter);
//                break;
            }
        }
        converters.addAll(tail);
        return converters;
    }
}
