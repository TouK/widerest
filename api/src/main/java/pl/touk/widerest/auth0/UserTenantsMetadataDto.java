package pl.touk.widerest.auth0;

import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

import java.util.List;

/**
 * Created by mst on 17.09.15.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName(value = "user_metadata")
public class UserTenantsMetadataDto {
    private List<String> tenants;
}
