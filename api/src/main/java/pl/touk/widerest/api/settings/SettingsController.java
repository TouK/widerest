package pl.touk.widerest.api.settings;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.Optional;
import java.util.stream.Collectors;

import lombok.Builder;
import org.broadleafcommerce.common.config.dao.SystemPropertiesDao;
import org.broadleafcommerce.common.config.domain.SystemProperty;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.Data;
import pl.touk.widerest.api.settings.converters.PropertyConverter;
import pl.touk.widerest.api.settings.dto.PropertyDto;
import pl.touk.widerest.security.config.ResourceServerConfig;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping(value = ResourceServerConfig.API_PATH + "/settings")
@Api(value = "settings", description = "System properties endpoint")
public class SettingsController {

    @javax.annotation.Resource(name = "blSystemPropertiesDao")
    protected SystemPropertiesDao systemPropertiesDao;

    @javax.annotation.Resource
    protected SettingsService settingsService;

    @javax.annotation.Resource
    protected PropertyConverter propertyConverter;

    @PreAuthorize("hasRole('PERMISSION_ALL_SYSTEM_PROPERTY')")
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation(
            value = "Get system setting value for a given key",
            notes = "",
            response = String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Value successfully returned"),
            @ApiResponse(code = 204, message = "Value for the key hasn't been set yet"),
            @ApiResponse(code = 404, message = "There is no system property for the key")
    })
    public Resources<PropertyDto> listAll() {
        return new Resources<>(
                settingsService.getAvailableSystemPropertyNames().stream()
                        .map(name -> propertyConverter.createDto(name, false))
                        .collect(Collectors.toList()),

                linkTo(methodOn(getClass()).listAll()).withSelfRel()
        );

    }

    @PreAuthorize("hasRole('PERMISSION_ALL_SYSTEM_PROPERTY')")
    @RequestMapping(value = "/{key}", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get system setting value for a given key",
            notes = "",
            response = String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Value successfully returned"),
            @ApiResponse(code = 204, message = "Value for the key hasn't been set yet"),
            @ApiResponse(code = 404, message = "There is no system property for the key")
    })
    public ResponseEntity getValue(@ApiParam @PathVariable("key") String key) {

        return Optional.of(key)
                .filter(settingsService.getAvailableSystemPropertyNames()::contains)
                .map(name -> Optional.ofNullable(systemPropertiesDao.readSystemPropertyByName(name))
                                        .map(SystemProperty::getValue)
                                        .map(ResponseEntity::ok)
                                        .map(ResponseEntity.class::cast)
                                        .orElse(ResponseEntity.noContent().build())
                )
                .orElse(ResponseEntity.notFound().build());

    }

    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_SYSTEM_PROPERTY')")
    @RequestMapping(value = "/{key}", method = RequestMethod.PUT)
    @ApiOperation(
            value = "Set client id used in PayPal",
            notes = "",
            response = Void.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Value successfully set"),
            @ApiResponse(code = 404, message = "There is no system property for the key")
    })
    public ResponseEntity setValue(
            @ApiParam @PathVariable("key") String key,
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @ApiParam @RequestBody String value
    ) {

        return Optional.of(key)
                .filter(settingsService.getAvailableSystemPropertyNames()::contains)
                .map(name -> {
                    SystemProperty systemProperty = Optional.ofNullable(systemPropertiesDao.readSystemPropertyByName(name))
                            .orElseGet(() -> {
                                SystemProperty property = systemPropertiesDao.createNewSystemProperty();
                                property.setName(name);
                                return property;
                            });
                    systemProperty.setValue(value);
                    systemProperty = systemPropertiesDao.saveSystemProperty(systemProperty);
                    systemPropertiesDao.removeFromCache(systemProperty);
                    return (ResponseEntity) ResponseEntity.ok(systemProperty.getValue());
                })
                .orElse(ResponseEntity.notFound().build());

    }

}
