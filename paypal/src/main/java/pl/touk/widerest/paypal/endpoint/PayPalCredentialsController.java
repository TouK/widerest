package pl.touk.widerest.paypal.endpoint;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.broadleafcommerce.common.config.dao.SystemPropertiesDao;
import org.broadleafcommerce.common.config.domain.SystemProperty;
import org.broadleafcommerce.common.config.service.SystemPropertiesService;
import org.broadleafcommerce.openadmin.server.security.service.AdminUserDetails;
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
import pl.touk.widerest.paypal.service.SystemProperitesServiceProxy;

import javax.annotation.Resource;
import java.util.Optional;

@Controller
@ResponseBody
@RequestMapping("/paypal")
public class PayPalCredentialsController {

    @Resource(name = "wdSystemProperties")
    private SystemProperitesServiceProxy spServiceProxy;


    @PreAuthorize("hasRole('PERMISSION_ALL_ADMIN_USER')")
    @RequestMapping(value = "/id", method = RequestMethod.GET)
    public String getPayPalClientId(@AuthenticationPrincipal UserDetails userDetails) {

        return Optional.ofNullable(spServiceProxy.getSystemPropertyByName(SystemProperitesServiceProxy.CLIENT_ID))
                .map(SystemProperty::getValue)
                .orElseThrow(() -> new ResourceNotFoundException("Property not set"));
    }

    @PreAuthorize("hasRole('PERMISSION_ALL_ADMIN_USER')")
    @RequestMapping(value = "/secret", method = RequestMethod.GET)
    public String getPayPalSecret(@AuthenticationPrincipal UserDetails userDetails) {

        return Optional.ofNullable(spServiceProxy.getSystemPropertyByName(SystemProperitesServiceProxy.SECRET))
                .map(SystemProperty::getValue)
                .orElseThrow(() -> new ResourceNotFoundException("Property not set"));
    }

    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_ADMIN_USER')")
    @RequestMapping(value = "/id", method = RequestMethod.POST)
    public ResponseEntity<?> setClientId(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody String clientId
    ) {

        spServiceProxy.setOrUpdatePropertyByName(SystemProperitesServiceProxy.CLIENT_ID, clientId);

        if(spServiceProxy.getSystemPropertyByName(SystemProperitesServiceProxy.CLIENT_ID).getValue().equals(clientId))
            return new ResponseEntity<>(HttpStatus.CREATED);

        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @Transactional
    @PreAuthorize("hasRole('PERMISSION_ALL_ADMIN_USER')")
    @RequestMapping(value = "/secret", method = RequestMethod.POST)
    public ResponseEntity<?> setSecret(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody String secret
    ) {

        spServiceProxy.setOrUpdatePropertyByName(SystemProperitesServiceProxy.CLIENT_ID, secret);

        if(spServiceProxy.getSystemPropertyByName(SystemProperitesServiceProxy.CLIENT_ID).getValue().equals(secret))
            return new ResponseEntity<>(HttpStatus.CREATED);

        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

}
