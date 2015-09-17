package pl.touk.widerest.multitenancy;

import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

import javax.xml.bind.annotation.XmlRootElement;
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
