package pl.touk.widerest.api.cart.dto;

import com.wordnik.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * Created by mst on 08.07.15.
 */
@Data
@ApiModel("cartAttributes")
public class CartAttributeDto {
    private String name;

    private String value;
}
