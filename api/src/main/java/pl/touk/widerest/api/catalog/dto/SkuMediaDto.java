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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "Sku Media", description = "Sku Media DTO resource representation")
public class SkuMediaDto extends ResourceSupport {

    @JsonIgnore
    private long mediaId;

    @ApiModelProperty(position = 0, value = "Title of the media", required = true, dataType = "java.lang.String")
    private String title;

    @ApiModelProperty(position = 1, value = "URL to the resource associated with this media", required = true, dataType = "java.lang.String")
    private String url;

    @ApiModelProperty(position = 2, value = "Attribute (alt) for HTML property of IMG", dataType = "java.lang.String")
    private String altText;

    @ApiModelProperty(position = 3, value = "Tags describing the media", dataType = "java.lang.String")
    private String tags;

    @ApiModelProperty(position = 4, value = "Key of the media", required = true, dataType = "java.lang.String",
            allowableValues = "[primary, alt1, alt2, alt3, alt4, alt5, alt6, alt7, alt8, alt9]")
    private String key;
}