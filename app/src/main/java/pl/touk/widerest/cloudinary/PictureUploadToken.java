package pl.touk.widerest.cloudinary;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

import java.util.UUID;

@ApiModel
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PictureUploadToken {

    private int timestamp;

    private UUID publicId;

    private String apiKey;

    private String signature;
}
