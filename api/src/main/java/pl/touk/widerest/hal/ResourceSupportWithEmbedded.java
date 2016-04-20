package pl.touk.widerest.hal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class ResourceSupportWithEmbedded extends ResourceSupport {

    private final List<EmbeddedResource> embeddedResources;

    public ResourceSupportWithEmbedded() {
        embeddedResources = new ArrayList<EmbeddedResource>();
    }

    @JsonProperty("embedded")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<EmbeddedResource> getEmbeddedResources() {
        return embeddedResources;
    }

    public void add(EmbeddedResource... embeddedResources) {
        this.embeddedResources.addAll(Arrays.asList(embeddedResources));
    }

    public void add(EmbeddedResource embedded) {
        Assert.notNull(embedded, "Resource must not be null!");
        this.embeddedResources.add(embedded);
    }
}
