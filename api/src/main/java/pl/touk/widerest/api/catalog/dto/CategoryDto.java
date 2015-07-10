package pl.touk.widerest.api.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.springframework.hateoas.ResourceSupport;

import java.util.List;

/**
 * Created by mst on 06.07.15.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
