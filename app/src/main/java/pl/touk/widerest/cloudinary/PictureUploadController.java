package pl.touk.widerest.cloudinary;

import com.cloudinary.Cloudinary;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import io.swagger.annotations.Api;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import pl.touk.widerest.api.settings.SettingsConsumer;
import pl.touk.widerest.api.settings.SettingsService;
import pl.touk.widerest.security.config.ResourceServerConfig;

import javax.annotation.Resource;
import java.util.Set;
import java.util.UUID;
@Controller
@RequestMapping(value = ResourceServerConfig.API_PATH + "/pictureUpload")
@Api(value = "pictureUpload", description = "Get image upload authorization token endpoint")
public class PictureUploadController implements SettingsConsumer {

    private static final String CLOUDINARY_API_KEY = "cloudinarytKey";
    private static final String CLOUDINARY_API_SECRET = "cloudinarySecret";

    @Resource
    private SettingsService service;
    @Resource
    private Cloudinary cloudinary;

//    @RequestMapping(method = RequestMethod.GET)

    private PictureUploadToken authorizePictureUpload() {

        UUID publicId = UUID.randomUUID();
        int timestamp = (int) (System.currentTimeMillis() / 1000L);

        String signature = cloudinary.apiSignRequest(
                ImmutableMap.of(
                        "public_id", publicId,
                        "timestamp", timestamp),
                service.getProperty(CLOUDINARY_API_SECRET).get()
        );

        return PictureUploadToken.builder()
                .apiKey(service.getProperty(CLOUDINARY_API_KEY))
                .publicId(publicId)
                .timestamp(timestamp)
                .signature(signature)
                .build();
    }

    @Override
    public void setSettingsService(SettingsService settingsService) {
        service = settingsService ;
    }

    @Override
    public Set<String> getHandledProperties() {
        return Sets.newHashSet(CLOUDINARY_API_KEY,CLOUDINARY_API_SECRET);
    }
}
