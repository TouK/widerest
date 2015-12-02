package pl.touk.widerest;


/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.ser.std.MapSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.ImmutableSet;
import org.springframework.hateoas.hal.*;

import java.io.IOException;
import java.util.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.RelProvider;
import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.stereotype.Component;

import javax.xml.bind.annotation.XmlElement;

/**
 * Jackson 2 module implementation to render {@link org.springframework.hateoas.Link} and {@link org.springframework.hateoas.ResourceSupport} instances in HAL compatible JSON.
 *
 * Extends this class to make it possible for a relationship to be serialized as an array even if there is only 1 link
 * This is done is in OptionalListJackson2Serializer::serialize method.
 *
 * Relationships to force as arrays are defined in relsToForceAsAnArray
 */

public class MultiLinkAwareJackson2HalModule extends Jackson2HalModule {

    private static final long serialVersionUID = 7806951456457932384L;

    private static final ImmutableSet<String> relsToForceAsAnArray = ImmutableSet.copyOf(Arrays.asList(
            "subcategories"
    ));

    private static abstract class MultiLinkAwareResourceSupportMixin extends ResourceSupport {

        @Override
        @XmlElement(name = "link")
        @JsonProperty("links")
        //here's the only diff from org.springframework.hateoas.hal.ResourceSupportMixin
        //we use a different HalLinkListSerializer
        @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY, using = MultiLinkAwareHalLinkListSerializer.class)
        @JsonDeserialize(using = MultiLinkAwareJackson2HalModule.HalLinkListDeserializer.class)
        public abstract List<Link> getLinks();
    }

    public MultiLinkAwareJackson2HalModule() {
        super();
        //NOTE: super calls setMixInAnnotation(Link.class, LinkMixin.class);
        //you must not override this as this is how Spring-HATEOAS determines if a
        //Hal converter has been registered for not.
        //If it determines a Hal converter has not been registered, it will register it's own
        //that will override this one

        //Use customized ResourceSupportMixin to use our LinkListSerializer
        setMixInAnnotation(ResourceSupport.class, MultiLinkAwareResourceSupportMixin.class);
    }


    public static class MultiLinkAwareHalLinkListSerializer extends Jackson2HalModule.HalLinkListSerializer {

        private final BeanProperty property;
        private final CurieProvider curieProvider;
        private final Set<String> relsAsMultilink;


        public MultiLinkAwareHalLinkListSerializer(BeanProperty property, CurieProvider curieProvider, Set<String> relsAsMultilink) {

            super(property, curieProvider);
            this.property = property;
            this.curieProvider = curieProvider;
            this.relsAsMultilink = relsAsMultilink;
        }

        @Override
        public void serialize(List<Link> value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
                JsonGenerationException {

            // sort links according to their relation
            Map<String, List<Object>> sortedLinks = new LinkedHashMap<String, List<Object>>();
            List<Link> links = new ArrayList<Link>();

            boolean prefixingRequired = curieProvider != null;
            boolean curiedLinkPresent = false;

            for (Link link : value) {

                String rel = prefixingRequired ? curieProvider.getNamespacedRelFrom(link) : link.getRel();

                if (!link.getRel().equals(rel)) {
                    curiedLinkPresent = true;
                }

                if (sortedLinks.get(rel) == null) {
                    sortedLinks.put(rel, new ArrayList<Object>());
                }

                links.add(link);
                sortedLinks.get(rel).add(link);
            }

            if (prefixingRequired && curiedLinkPresent) {

                ArrayList<Object> curies = new ArrayList<Object>();
                curies.add(curieProvider.getCurieInformation(new Links(links)));

                sortedLinks.put("curies", curies);
            }

            TypeFactory typeFactory = provider.getConfig().getTypeFactory();
            JavaType keyType = typeFactory.uncheckedSimpleType(String.class);
            JavaType valueType = typeFactory.constructCollectionType(ArrayList.class, Object.class);
            JavaType mapType = typeFactory.constructMapType(HashMap.class, keyType, valueType);

            //CHANGE HERE: only thing we are changing ins the List Serializer
            //shame there's not a better way to override this very specific behaviour
            //without copy pasta the whole class
            MapSerializer serializer = MapSerializer.construct(new String[] {}, mapType, true, null,
                    provider.findKeySerializer(keyType, null), new MultiLinkAwareOptionalListJackson2Serializer(property, relsAsMultilink), null);

            serializer.serialize(sortedLinks, jgen, provider);
        }

        public MultiLinkAwareHalLinkListSerializer withForcedRels(String[] relationships) {
            ImmutableSet<String> relsToForce =  ImmutableSet.<String>builder().addAll(this.relsAsMultilink).add(relationships).build();
            return new MultiLinkAwareHalLinkListSerializer(this.property, this.curieProvider, relsToForce);
        }

        @Override
        public JsonSerializer<?> createContextual(SerializerProvider provider, BeanProperty property)
                throws JsonMappingException {
            return new MultiLinkAwareHalLinkListSerializer(property, curieProvider, this.relsAsMultilink);
        }
    }


    public static class MultiLinkAwareOptionalListJackson2Serializer extends Jackson2HalModule.OptionalListJackson2Serializer {

        private final BeanProperty property;
        private final Map<Class<?>, JsonSerializer<Object>> serializers;
        private final Set<String> relsAsMultilink;

        public MultiLinkAwareOptionalListJackson2Serializer(BeanProperty property, Set<String> relsAsMultilink) {
            super(property);
            this.property = property;
            this.serializers = new HashMap<Class<?>, JsonSerializer<Object>>();
            this.relsAsMultilink = relsAsMultilink;
        }

        @Override
        public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
                JsonGenerationException {

            List<?> list = (List<?>) value;

            if (list.isEmpty()) {
                return;
            }

            if(list.get(0) instanceof  Link) {
                Link link = (Link) list.get(0);
                String rel = link.getRel();

                if (list.size() > 1 || relsAsMultilink.contains(rel)) {
                    jgen.writeStartArray();
                    serializeContents(list.iterator(), jgen, provider);
                    jgen.writeEndArray();
                } else {
                    serializeContents(list.iterator(), jgen, provider);
                }
            }
        }

        private void serializeContents(Iterator<?> value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException, JsonGenerationException {

            while (value.hasNext()) {
                Object elem = value.next();
                if (elem == null) {
                    provider.defaultSerializeNull(jgen);
                } else {
                    getOrLookupSerializerFor(elem.getClass(), provider).serialize(elem, jgen, provider);
                }
            }
        }

        private JsonSerializer<Object> getOrLookupSerializerFor(Class<?> type, SerializerProvider provider)
                throws JsonMappingException {

            JsonSerializer<Object> serializer = serializers.get(type);

            if (serializer == null) {
                serializer = provider.findValueSerializer(type, property);
                serializers.put(type, serializer);
            }

            return serializer;
        }

        @Override
        public JsonSerializer<?> createContextual(SerializerProvider provider, BeanProperty property)
                throws JsonMappingException {
            return new MultiLinkAwareOptionalListJackson2Serializer(property, relsAsMultilink);
        }
    }


    public static class MultiLinkAwareHalHandlerInstantiator extends Jackson2HalModule.HalHandlerInstantiator {

        private final MultiLinkAwareHalLinkListSerializer linkListSerializer;

        public MultiLinkAwareHalHandlerInstantiator(RelProvider resolver, CurieProvider curieProvider) {
            super(resolver, curieProvider, true);
            this.linkListSerializer = new MultiLinkAwareHalLinkListSerializer(null, curieProvider, relsToForceAsAnArray);
        }

        @Override
        public JsonSerializer<?> serializerInstance(SerializationConfig config, Annotated annotated, Class<?> serClass) {
            if(serClass.equals(MultiLinkAwareHalLinkListSerializer.class)){
                if (annotated.hasAnnotation(ForceMultiLink.class)) {
                    return this.linkListSerializer.withForcedRels(annotated.getAnnotation(ForceMultiLink.class).value());
                } else {
                    return this.linkListSerializer;
                }

            } else {
                return super.serializerInstance(config, annotated, serClass);
            }

        }

    }

}