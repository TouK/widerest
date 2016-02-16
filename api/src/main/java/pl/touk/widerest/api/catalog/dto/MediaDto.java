package pl.touk.widerest.api.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName("media")
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Media", description = "Media DTO resource representation")
public class MediaDto extends BaseDto {

    @ApiModelProperty(position = 0, value = "Title of the media", required = true, dataType = "java.lang.String")
    private String title;

    @ApiModelProperty(position = 1, value = "URL to the resource associated with this media", required = true, dataType = "java.lang.String")
    private String url;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 2, value = "Attribute (alt) for HTML property of IMG", dataType = "java.lang.String")
    private String altText;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 3, value = "Tags describing the media", dataType = "java.lang.String")
    private String tags;

//    @ApiModelProperty(position = 4, value = "Key of the media", required = true, dataType = "java.lang.String")/*,
//            allowableValues = "[\"primary\", \"alt1\", \"alt2\", \"alt3\", \"alt4\", \"alt5\", \"alt6\", \"alt7\", \"alt8\", \"alt9\"]")*/
//    private String key;
}
