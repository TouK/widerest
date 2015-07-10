package pl.touk.widerest.api.catalog.dto;

import lombok.Data;
import lombok.experimental.Builder;
import pl.touk.widerest.api.cart.dto.CustomerDto;

/**
 * Created by mst on 10.07.15.
 */
@Data
@Builder
public class RatingDto {
    private CustomerDto customer;

    private Double rating;


}
