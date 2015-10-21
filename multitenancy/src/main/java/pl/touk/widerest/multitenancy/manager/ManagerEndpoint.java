package pl.touk.widerest.multitenancy.manager;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.broadleafcommerce.common.security.util.PasswordChange;
import org.broadleafcommerce.common.service.GenericResponse;
import org.broadleafcommerce.openadmin.server.security.service.AdminSecurityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pl.touk.widerest.security.config.ResourceServerConfig;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.Resource;
import java.util.Optional;

@RestController
@RequestMapping(value = ResourceServerConfig.API_PATH + "/manager/admin", produces = "application/json")
public class ManagerEndpoint {

    @Resource(name="blAdminSecurityService")
    private AdminSecurityService adminSecurityService;


    @PreAuthorize("hasRole('PERMISSION_ALL_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, consumes = "text/plain")
    @ApiOperation(
            value = "Change admin password",
            notes = "Changes password of a currently logged in admin user. Any constraints and further password " +
                    "verification should by done in front-end.",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful update of admin's password"),
            @ApiResponse(code = 400, message = "Not enough data has been provided"),
            @ApiResponse(code = 404, message = "Currently logged in admin user does not exist (???)"),
            @ApiResponse(code = 409, message = "New password matches the old one")
    })
    public ResponseEntity<?> changeAdminUserPassword(
            @ApiIgnore @AuthenticationPrincipal UserDetails userDetails,
            @ApiParam(value = "New Admin password", required = true)
                @RequestBody String newPassword) {

        /* (mst) Other contraints at front-end side */
        if(newPassword == null || newPassword.isEmpty()) {
            throw new MissingRequiredDataException();
        }

        if(userDetails.getPassword().equals(newPassword)) {
            throw new PasswordMatchingException();
        }

        Optional.ofNullable(adminSecurityService.readAdminUserByUserName(userDetails.getUsername()))
                .orElseThrow(AdminUserNotFoundException::new);

        final PasswordChange passwordChange = new PasswordChange(userDetails.getUsername());
        passwordChange.setNewPassword(newPassword);

        adminSecurityService.changePassword(passwordChange);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/{admin}", method = RequestMethod.POST, consumes = "application/json")
    @ApiOperation(
            value = "Change password of a specified admin user",
            notes = "Changes password of a specified admin user. Any constraints and further password " +
                    "verification should by done in front-end.",
            response = Void.class)
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successful update of admin's password"),
            @ApiResponse(code = 400, message = "Not enough data has been provided"),
            @ApiResponse(code = 404, message = "Specified admin user does not exist or password change did not succeed"),
            @ApiResponse(code = 409, message = "New password matches the old one")
    })
    public ResponseEntity<?> changeUserPasswordByName(
            @ApiParam(value = "Name of a specfic admin user", required = true)
                @PathVariable(value = "admin") String adminUserName,
            @ApiParam(value = "User Credentials", required = true)
                @RequestBody NewPasswordCredentialsDto newPasswordCredentialsDto) {

        if(newPasswordCredentialsDto == null  || newPasswordCredentialsDto.getCurrentPassword() == null || newPasswordCredentialsDto.getCurrentPassword().isEmpty() ||
                newPasswordCredentialsDto.getNewPassword() == null /* (mst) New password can be empty I guess...*/) {
            throw new MissingRequiredDataException();
        }

        if(newPasswordCredentialsDto.getCurrentPassword().equals(newPasswordCredentialsDto.getNewPassword())) {
            throw new PasswordMatchingException();
        }

        Optional.ofNullable(adminSecurityService.readAdminUserByUserName(adminUserName))
                .orElseThrow(AdminUserNotFoundException::new);

        final GenericResponse changePasswordResponse = adminSecurityService.changePassword(adminUserName, newPasswordCredentialsDto.getCurrentPassword(), newPasswordCredentialsDto.getNewPassword(),
                newPasswordCredentialsDto.getNewPassword());

        if(!changePasswordResponse.getHasErrors()) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }


    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    static class AdminUserNotFoundException extends RuntimeException {}

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    static class MissingRequiredDataException extends RuntimeException {}

    @ResponseStatus(value = HttpStatus.CONFLICT)
    static class PasswordMatchingException extends RuntimeException {}



}
