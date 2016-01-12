package pl.touk.widerest.api;

import com.fasterxml.jackson.annotation.JsonRootName;
import org.atteo.evo.inflector.English;
import org.springframework.hateoas.RelProvider;
import org.springframework.hateoas.core.DefaultRelProvider;

import java.lang.annotation.Annotation;

public class JsonRootRelProvider implements RelProvider {

    final DefaultRelProvider defaultRelProvider = new DefaultRelProvider();

    @Override
    public String getItemResourceRelFor(Class<?> type) {
        final Annotation[] jsonAnnotations = type.getAnnotationsByType(JsonRootName.class);

        return (jsonAnnotations != null && jsonAnnotations.length > 0) ?
                ((JsonRootName)jsonAnnotations[0]).value() :
                defaultRelProvider.getItemResourceRelFor(type);
    }

    @Override
    public String getCollectionResourceRelFor(Class<?> type) {
        final Annotation[] jsonAnnotations = type.getAnnotationsByType(JsonRootName.class);

        return (jsonAnnotations != null && jsonAnnotations.length > 0) ?
                English.plural(((JsonRootName)jsonAnnotations[0]).value()) :
                English.plural(defaultRelProvider.getCollectionResourceRelFor(type));
    }

    @Override
    public boolean supports(Class<?> delimiter) {
        return defaultRelProvider.supports(delimiter);
    }
}
