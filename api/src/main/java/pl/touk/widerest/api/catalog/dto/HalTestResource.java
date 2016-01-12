package pl.touk.widerest.api.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.experimental.Builder;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;

@Data
@Builder
@JsonRootName("halTestResource")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HalTestResource {

    private String name;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    private Resources<Resource<HalTestResource>> subResource;
}
