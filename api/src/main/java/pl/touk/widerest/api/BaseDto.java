package pl.touk.widerest.api;

import io.swagger.annotations.ApiModelProperty;
import org.springframework.hateoas.EmbeddedResource;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;

import java.util.List;

public class BaseDto extends ResourceSupport {

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
