package pl.touk.widerest.api;

import io.swagger.annotations.ApiModelProperty;
import org.springframework.hateoas.Link;
import pl.touk.widerest.hal.EmbeddedResource;
import pl.touk.widerest.hal.ResourceSupportWithEmbedded;

import java.util.List;

public class BaseDto extends ResourceSupportWithEmbedded {

    @Override
    @ApiModelProperty(hidden = true)
    public List<Link> getLinks() {
        return super.getLinks();
    }

    @Override
    @ApiModelProperty(hidden = true)
    public List<EmbeddedResource> getEmbeddedResources() {
        return super.getEmbeddedResources();
    }
}
