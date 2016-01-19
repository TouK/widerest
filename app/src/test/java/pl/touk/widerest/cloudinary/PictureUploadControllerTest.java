package pl.touk.widerest.cloudinary;

import com.cloudinary.Cloudinary;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.touk.widerest.api.settings.SettingsService;

import java.util.Optional;
import java.util.Random;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PictureUploadControllerTest {

    @Mock
    final SettingsService service = mock(SettingsService.class);

    @Mock
    final Cloudinary cloudinary = mock(Cloudinary.class);

    @InjectMocks
    private PictureUploadController pictureUploadController = new PictureUploadController();

    @Test
    public void shouldGetToken() throws PictureUploadAuthorizationException {
        //given
        final String signature = String.valueOf(new Random().nextInt());
        when(cloudinary.apiSignRequest(any(), any())).thenReturn(signature);

        //when
        PictureUploadToken token = pictureUploadController.getToken();

        //then
        assertThat(token.getSignature().contains(signature), Matchers.equalTo(true));

    }

    @Test(expected = PictureUploadAuthorizationException.class)
    public void shouldThrowExceptionOnEmptyCredentials() throws PictureUploadAuthorizationException {
        //given
        when(service.getProperty(PictureUploadController.CLOUDINARY_API_KEY)).thenReturn(Optional.empty());
        when(service.getProperty(PictureUploadController.CLOUDINARY_API_SECRET)).thenReturn(Optional.empty());

        //when
        PictureUploadToken token = pictureUploadController.getToken();

    }

    @Test
    public void shouldGetHandledProperties() {
        assertThat(pictureUploadController.getHandledProperties().contains(PictureUploadController.CLOUDINARY_API_KEY), Matchers.equalTo(true));
        assertThat(pictureUploadController.getHandledProperties().contains(PictureUploadController.CLOUDINARY_API_SECRET), Matchers.equalTo(true));

    }

}
