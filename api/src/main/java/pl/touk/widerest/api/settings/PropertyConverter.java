package pl.touk.widerest.api.settings;

import org.broadleafcommerce.common.config.dao.SystemPropertiesDao;
import org.broadleafcommerce.common.config.domain.SystemProperty;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import pl.touk.widerest.api.Converter;

import javax.annotation.Resource;
import java.util.Optional;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class PropertyConverter implements Converter<String, PropertyDto> {

    @Resource(name = "blSystemPropertiesDao")
    protected SystemPropertiesDao systemPropertiesDao;

    @Override
    public PropertyDto createDto(final String propertyName, final boolean embed) {
        final PropertyDto propertyDto = PropertyDto.builder()
                .name(propertyName)
                .value(
                        Optional.ofNullable(systemPropertiesDao.readSystemPropertyByName(propertyName))
                                .map(SystemProperty::getValue)
                                .orElse(null)
                )
                .build();

        propertyDto.add(ControllerLinkBuilder.linkTo(methodOn(SettingsController.class).getValue(propertyDto.getName())).withSelfRel());

        return propertyDto;
    }

    @Override
    public String createEntity(final PropertyDto propertyDto) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String updateEntity(final String s, final PropertyDto propertyDto) {
        throw new UnsupportedOperationException();
    }
}
