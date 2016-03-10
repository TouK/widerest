package pl.touk.widerest.api.products;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;
import pl.touk.widerest.api.BaseDto;
import pl.touk.widerest.api.common.MediaDto;
import pl.touk.widerest.api.products.skus.SkuDto;
import pl.touk.widerest.api.products.skus.SkuProductOptionValueDto;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName("product")
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
public class ProductDto extends BaseDto {

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 0, value = "Names of the categories this product belongs to", dataType = "java.lang.String")
    private String categoryName;

    @NotBlank(message = "Product has to have a non empty name")
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



    // ------------------- Replaces defaultSku

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 6, value = "Default sale price of this product", required = true, dataType = "java.math.BigDecimal")
    @DecimalMin("0.01")
    private BigDecimal salePrice;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 7, value = "Default retail price of this product", dataType = "java.math.BigDecimal")
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal retailPrice;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @ApiModelProperty(position = 8, value = "Available quantity of the product", required = true, dataType = "java.lang.Integer")
    private Integer quantityAvailable;

    @ApiModelProperty(position = 9, value = "Product's availability", dataType = "java.lang.String")
    private String availability;

    @ApiModelProperty(position = 10, value = "Product's sale/retail price currency", required = true, dataType = "java.lang.String")
    private String currencyCode;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 11, value = "Tax code for this product", dataType = "java.lang.String")
    private String taxCode;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 12, value = "Default attributes associated with this product")
    private Map<String, String> skuAttributes;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 13, value = "A set of selected product's option values", dataType = "java.util.Set")
    private Set<SkuProductOptionValueDto> skuProductOptionValues;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 14, value = "List of medias associated with this product", dataType = "java.util.List")
    private Map<String /*key*/, MediaDto> media;

    // ------------------- Replaces defaultSku

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 15, value = "List of all additional SKUs for the product", dataType = "java.util.List")
    @Valid
    private List<SkuDto> skus;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 16, value = "Date from which the product becomes active/valid", dataType = "java.util.Date")
    private Date validFrom;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 17, value = "Date from which the product becomes inactive/invalid", dataType = "java.util.Date")
    private Date validTo;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 18, value = "Offer message for the product", dataType = "java.lang.String")
    private String offerMessage;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 19, value = "Model of the product", dataType = "java.lang.String")
    private String model;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 20, value = "Manufacturer of the product", dataType = "java.lang.String")
    private String manufacturer;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    @ApiModelProperty(position = 21, dataType = "java.lang.String")
    private String url;
}