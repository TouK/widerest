package pl.touk.widerest.hal;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom {@link JsonSerializer} to render {@link EmbeddedResource}s in HAL compatible JSON. Renders the list as a Map.
 *
 * @author Tomasz Wielga
 */
public class HalEmbeddedResourcesSerializer extends ContainerSerializer<Collection<EmbeddedResource>> implements ContextualSerializer {

    private final BeanProperty property;

    public HalEmbeddedResourcesSerializer() {
        this(null);
    }

    public HalEmbeddedResourcesSerializer(BeanProperty property) {

        super(Collection.class, false);

        this.property = property;
    }

    @Override
    public void serialize(Collection<EmbeddedResource> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException {

        Map<String, Object> embeddeds = new HashMap<String, Object>();
        for (EmbeddedResource embedded : value) {
            embeddeds.put(embedded.getRel(), embedded.getResource());
        }

        Object currentValue = jgen.getCurrentValue();

        provider.findValueSerializer(Map.class, property).serialize(embeddeds, jgen, provider);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        return new HalEmbeddedResourcesSerializer(property);
    }

    @Override
    public JavaType getContentType() {
        return null;
    }

    @Override
    public JsonSerializer<EmbeddedResource> getContentSerializer() {
        return null;
    }

    public boolean isEmpty(Collection<EmbeddedResource> value) {
        return isEmpty(null, value);
    }

    public boolean isEmpty(SerializerProvider provider, Collection<EmbeddedResource> value) {
        return value.isEmpty();
    }

    @Override
    public boolean hasSingleElement(Collection<EmbeddedResource> value) {
        return value.size() == 1;
    }

    @Override
    protected ContainerSerializer<EmbeddedResource> _withValueTypeSerializer(TypeSerializer vts) {
        return null;
    }
}