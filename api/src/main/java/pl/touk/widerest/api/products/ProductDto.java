package pl.touk.widerest.api.products;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
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

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonRootName("product")
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

    @ApiModelProperty(position = 0, value = "Names of the categories this product belongs to", dataType = "java.lang.String")
    private String categoryName;

    @NotBlank(message = "Product has to have a non empty name")
    @ApiModelProperty(position = 1, value = "Name of the product", required = true, dataType = "java.lang.String")
    private String name;

    @ApiModelProperty(position = 2, value = "Short description of the product", dataType = "java.lang.String")
    private String description;

    @ApiModelProperty(position = 3, value = "Long description of the product", dataType = "java.lang.String")
    private String longDescription;

    @ApiModelProperty(position = 4, value = "Map of attributes further describing the product")
    private Map<String, String> attributes;

    @ApiModelProperty(position = 5, value = "List of available options for the product. Used for creating/generating SKUs", dataType = "java.util.List")
    private List<ProductOptionDto> options;



    // ------------------- Replaces defaultSku

    @ApiModelProperty(position = 6, value = "Default sale price of this product", required = true, dataType = "java.math.BigDecimal")
    @DecimalMin("0.01")
    private BigDecimal salePrice;

    @ApiModelProperty(position = 7, value = "Default retail price of this product", dataType = "java.math.BigDecimal")
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal retailPrice;

    @ApiModelProperty(position = 8, value = "Available quantity of the product", required = true)
    private Integer quantityAvailable;

    @ApiModelProperty(position = 9, value = "Product's availability")
    private String availability;

    @ApiModelProperty(value = "SKU's basic availability (true/false)")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean isAvailable;

    @ApiModelProperty(position = 10, value = "Product's sale/retail price currency", required = true, dataType = "java.lang.String")
    private String currencyCode;

    @ApiModelProperty(position = 11, value = "Tax code for this product", dataType = "java.lang.String")
    private String taxCode;

    @ApiModelProperty(position = 14, value = "List of medias associated with this product", dataType = "java.util.List")
    private Map<String /*key*/, MediaDto> media;

    // ------------------- Replaces defaultSku

    @ApiModelProperty(position = 15, value = "List of all additional SKUs for the product", dataType = "java.util.List")
    @Valid
    private List<SkuDto> skus;

    @ApiModelProperty(position = 16, value = "Date from which the product becomes active/valid", dataType = "java.util.Date")
    private ZonedDateTime validFrom;

    @ApiModelProperty(position = 17, value = "Date from which the product becomes inactive/invalid", dataType = "java.util.Date")
    private ZonedDateTime validTo;

    @ApiModelProperty(position = 18, value = "Offer message for the product", dataType = "java.lang.String")
    private String offerMessage;

    @ApiModelProperty(position = 19, value = "Model of the product", dataType = "java.lang.String")
    private String model;

    @ApiModelProperty(position = 20, value = "Manufacturer of the product", dataType = "java.lang.String")
    private String manufacturer;

    @ApiModelProperty(position = 21, dataType = "java.lang.String")
    private String url;
}