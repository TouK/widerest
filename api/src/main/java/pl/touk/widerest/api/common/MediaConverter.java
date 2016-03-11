package pl.touk.widerest.api.common;

import org.broadleafcommerce.common.media.domain.Media;
import org.broadleafcommerce.common.media.domain.MediaImpl;
import org.broadleafcommerce.openadmin.web.service.MediaBuilderService;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;

@Component
public class MediaConverter implements Converter<Media, MediaDto> {

    @Override
    public MediaDto createDto(final Media media, boolean embed, boolean link) {
        return MediaDto.builder()
                .title(media.getTitle())
                .url(media.getUrl())
                .altText(media.getAltText())
                .tags(media.getTags())
                .build();
    }

    @Override
    public Media createEntity(final MediaDto mediaDto) {
        return updateEntity(new MediaImpl(), mediaDto);
    }

    @Override
    public Media updateEntity(final Media media, final MediaDto mediaDto) {
        media.setTitle(mediaDto.getTitle());
        media.setTags(mediaDto.getTags());
        media.setAltText(mediaDto.getAltText());
        media.setUrl(mediaDto.getUrl());

        return media;
    }
}
