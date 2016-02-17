package pl.touk.widerest.cloudinary;

import java.nio.file.AccessDeniedException;

public class PictureUploadAuthorizationException extends AccessDeniedException {
    public PictureUploadAuthorizationException(String file) {
        super(file);
    }
}
