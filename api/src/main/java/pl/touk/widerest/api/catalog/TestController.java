package pl.touk.widerest.api.catalog;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@RestController
@RequestMapping("/test")
public class TestController {

    @RequestMapping("/none")
    public String none() {
        return "FAIL";
    }

    @RequestMapping("/all")
    @PreAuthorize("permitAll")
    public String all() {
        return "OK";
    }

    @RequestMapping("/anonymous")
    @PreAuthorize("isAnonymous()")
    public String anonymous() {
        return "OK";
    }

    @RequestMapping("/registered")
    @PreAuthorize("isFullyAuthenticated()")
    public String registered() {
        return "OK";
    }

    @RequestMapping("/admin")
    @PreAuthorize("isFullyAythenticated()")
    public String admin() {
        return "OK";
    }

}