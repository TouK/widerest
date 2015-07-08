package pl.touk.widerest.api.cart.controllers;

import org.broadleafcommerce.core.web.controller.cart.AbstractCartController;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import pl.touk.widerest.api.cart.dto.CartDto;

import java.security.Principal;

/**
 * Created by mst on 07.07.15.
 */
@RestController
@RequestMapping("/order")
public class OrderController /*extends AbstractCartController */ {


    //@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @RequestMapping(method = RequestMethod.GET)
    public String getCartItems(OAuth2Authentication oAuth2Authentication) {

        //CustomerUserDetails userDetails = (CustomerUserDetails) oAuth2Authentication.getPrincipal();

        //return oAuth2Authentication.getName() + " a id: " + userDetails.getId();

        if(oAuth2Authentication == null) {
            return "AUTH!";
        }

        return "OK-p";
    }
}
