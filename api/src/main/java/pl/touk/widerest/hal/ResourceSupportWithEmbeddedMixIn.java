package pl.touk.widerest.hal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.hal.Jackson2HalModule;

import javax.xml.bind.annotation.XmlElement;
import java.util.List;

public abstract class ResourceSupportWithEmbeddedMixIn extends ResourceSupportWithEmbedded {

    @Override
    @XmlElement(name = "link")
    @JsonProperty("_links")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonSerialize(using = Jackson2HalModule.HalLinkListSerializer.class)
    @JsonDeserialize(using = Jackson2HalModule.HalLinkListDeserializer.class)
    public abstract List<Link> getLinks();


    @Override
    @XmlElement(name = "embedded")
    @JsonProperty("_embedded")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonSerialize(using = HalEmbeddedResourcesSerializer.class)
    public abstract List<EmbeddedResource> getEmbeddedResources();

}
