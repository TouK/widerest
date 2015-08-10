package pl.touk.widerest.api.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;
import org.springframework.hateoas.ResourceSupport;

/**
 * Created by mst on 07.08.15.
 */
@ApiModel
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SkuMediaDto extends ResourceSupport {

    @JsonIgnore
    private long mediaId;

    @ApiModelProperty(required = true)
    private String title;

    @ApiModelProperty(required = true)
    private String url;

    @ApiModelProperty(required = true)
    private String altText;

    @ApiModelProperty
    private String tags;
}