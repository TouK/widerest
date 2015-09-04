package pl.touk.widerest.paypal.endpoint;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.broadleafcommerce.common.config.domain.SystemProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import pl.touk.widerest.paypal.exception.CredentialNotSetException;
import pl.touk.widerest.paypal.service.SystemPropertiesServiceProxy;

import javax.annotation.Resource;
import java.util.Optional;

@Controller
@ResponseBody
@RequestMapping("/paypal")
@Api(value = "paypal-credentials", description = "PayPal credentials endpoint")
public class PayPalCredentialsController {

    @Resource(name = "wdSystemProperties")
    private SystemPropertiesServiceProxy spServiceProxy;


    @PreAuthorize("hasRole('PERMISSION_ALL_ADMIN_USER')")
    @RequestMapping(value = "/id", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get client id used in PayPal",
            notes = "",
            response = String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Client ID successfully received"),
            @ApiResponse(code = 500, message = "Client ID hasn't been set yet")
    })
    public String getPayPalClientId(@AuthenticationPrincipal UserDetails userDetails) {

        return Optional.ofNullable(spServiceProxy.getSystemPropertyByName(SystemPropertiesServiceProxy.CLIENT_ID))
                .map(SystemProperty::getValue)
                .orElseThrow(() -> new CredentialNotSetException("Property not set"));
    }

    @PreAuthorize("hasRole('PERMISSION_ALL_ADMIN_USER')")
    @RequestMapping(value = "/secret", method = RequestMethod.GET)
    @ApiOperation(
            value = "Get client secret used in PayPal",
            notes = "",
            response = String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Client secret successfully received"),
            @ApiResponse(code = 500, message = "Client secret hasn't been set yet")
    })
    public String getPayPalSecret(@AuthenticationPrincipal UserDetails userDetails) {

        return Optional.ofNullable(spServiceProxy.getSystemPropertyByName(SystemPropertiesServiceProxy.SECRET))
                .map(SystemProperty::getValue)
                .orElseThrow(() -> new CredentialNotSetException("Property not set"));
    }

    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_ADMIN_USER')")
    @RequestMapping(value = "/id", method = RequestMethod.POST)
    @ApiOperation(
            value = "Set client id used in PayPal",
            notes = "",
            response = Void.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Client ID successfully set"),
            @ApiResponse(code = 500, message = "Client ID cannot be set")
    })
    public ResponseEntity<?> setClientId(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody String clientId
    ) {

        spServiceProxy.setOrUpdatePropertyByName(SystemPropertiesServiceProxy.CLIENT_ID, clientId);

        if(spServiceProxy.getSystemPropertyByName(SystemPropertiesServiceProxy.CLIENT_ID).getValue().equals(clientId))
            return new ResponseEntity<>(HttpStatus.CREATED);

        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_ADMIN_USER')")
    @RequestMapping(value = "/secret", method = RequestMethod.POST)
    @ApiOperation(
            value = "Set client secret used in PayPal",
            notes = "",
            response = Void.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Client secret successfully set"),
            @ApiResponse(code = 500, message = "Client secret cannot be set")
    })
    public ResponseEntity<?> setSecret(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody String secret
    ) {

        spServiceProxy.setOrUpdatePropertyByName(SystemPropertiesServiceProxy.SECRET, secret);

        if(spServiceProxy.getSystemPropertyByName(SystemPropertiesServiceProxy.SECRET).getValue().equals(secret))
            return new ResponseEntity<>(HttpStatus.CREATED);

        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
