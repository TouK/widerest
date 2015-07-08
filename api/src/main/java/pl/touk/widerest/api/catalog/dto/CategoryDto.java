package pl.touk.widerest.api.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Builder;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.springframework.hateoas.ResourceSupport;

import java.util.List;

/**
 * Created by mst on 06.07.15.
 */

@Data
@Builder
@ApiModel
public class CategoryDto extends ResourceSupport {


    @JsonIgnore
    private Long categoryId;

    @ApiModelProperty(required = true)
    private String name;

    @ApiModelProperty(required = true)
    private String description;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    private String longDescription;
}
