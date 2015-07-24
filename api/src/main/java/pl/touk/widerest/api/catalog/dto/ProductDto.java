package pl.touk.widerest.api.catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.springframework.hateoas.ResourceSupport;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by mst on 06.07.15.
 */
@ApiModel

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        defaultImpl = ProductDto.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ProductDto.class, name = "product"),
        @JsonSubTypes.Type(value = BundleDto.class, name = "bundle")})

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDto extends ResourceSupport {
    @JsonIgnore
    private Long productId;

    //@JsonIgnore
    //@ApiModelProperty(required = true)
    //private CategoryDto category;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String categoryName;

    @ApiModelProperty(required = true)
    private String name;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String description;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String longDescription;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private Map<String, String> attributes;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    List<ProductOptionDto> options;

    @ApiModelProperty(required = true)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private SkuDto defaultSku;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private List<SkuDto> skus;

    /* TODO: Implement Possbile Blundles */
    //@JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    //@ApiModelProperty
    //private List<Long> possibleBundles;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private Date validFrom;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private Date validTo;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String offerMessage;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String model;

    @ApiModelProperty
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String manufacturer;
}
