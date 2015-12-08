package pl.touk.widerest.api.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;
import org.springframework.hateoas.ResourceSupport;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by mst on 06.07.15.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        defaultImpl = ProductDto.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ProductDto.class, name = "product"),
        @JsonSubTypes.Type(value = ProductBundleDto.class, name = "bundle")
})
@ApiModel(value = "Product", description = "Product DTO resource representation")
public class ProductDto extends ResourceSupport {

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 0, value = "Names of the categories this product belongs to", dataType = "java.lang.String")
    private String categoryName;

    @ApiModelProperty(position = 1, value = "Name of the product", required = true, dataType = "java.lang.String")
    private String name;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 2, value = "Short description of the product", dataType = "java.lang.String")
    private String description;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 3, value = "Long description of the product", dataType = "java.lang.String")
    private String longDescription;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 4, value = "Map of attributes further describing the product")
    private Map<String, String> attributes;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 5, value = "List of available options for the product. Used for creating/generating SKUs", dataType = "java.util.List")
    private List<ProductOptionDto> options;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 6, value = "Default SKU associated with the product", required = true, dataType = "pl.touk.widerest.api.catalog.dto.SkuDto")
    private SkuDto defaultSku;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 7, value = "List of all additional SKUs for the product", dataType = "java.util.List")
    private List<SkuDto> skus;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 8, value = "Date from which the product becomes active/valid", dataType = "java.util.Date")
    private Date validFrom;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 9, value = "Date from which the product becomes inactive/invalid", dataType = "java.util.Date")
    private Date validTo;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 10, value = "Offer message for the product", dataType = "java.lang.String")
    private String offerMessage;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 11, value = "Model of the product", dataType = "java.lang.String")
    private String model;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 12, value = "Manufacturer of the product", dataType = "java.lang.String")
    private String manufacturer;

//    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
//    @ApiModelProperty(position = 13, value = "Possible bundles for the product", dataType = "java.util.List")
//    private List<Long> possibleBundles;
}