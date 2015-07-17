package pl.touk.widerest.security.authentication;

import org.broadleafcommerce.openadmin.server.security.service.AdminUserDetails;
import org.broadleafcommerce.openadmin.server.security.service.AdminUserDetailsServiceImpl;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.broadleafcommerce.profile.core.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.provider.token.UserAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class UserAuthenticationConverterImpl implements UserAuthenticationConverter {

    public static final String ISS = "iss";
    public static final String SUB = "sub";

    public static final String BACKOFFICE = "widerest/backoffice";
    public static final String SITE = "widerest/site";

    @Autowired
    private AdminUserDetailsServiceImpl backofficeUserDetailsService;

    @Autowired
    private UserDetailsServiceImpl siteUserDetailsService;

    public Map<String, ?> convertUserAuthentication(Authentication authentication) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        Object principal = authentication.getPrincipal();
        if (principal instanceof AdminUserDetails) {
            response.put(ISS, BACKOFFICE);
            response.put(SUB, ((AdminUserDetails) principal).getUsername());
        } else if (principal instanceof CustomerUserDetails){
            response.put(ISS, SITE);
            response.put(SUB, ((CustomerUserDetails) principal).getUsername());
        }

        return response;
    }

    @Transactional
    public Authentication extractAuthentication(Map<String, ?> map) {
        if (map.containsKey(SUB)) {

            Object principal = map.get(SUB);
            String issuer = String.valueOf(map.get(ISS));

            if (siteUserDetailsService != null && SITE.equals(issuer)) {
                UserDetails userDetails = siteUserDetailsService.loadUserByUsername(String.valueOf(principal));
                return new SiteAuthenticationToken(userDetails, userDetails.getPassword(), userDetails.getAuthorities());
            }

            if (backofficeUserDetailsService != null && BACKOFFICE.equals(issuer)) {
                UserDetails userDetails = backofficeUserDetailsService.loadUserByUsername(String.valueOf(principal));
                return new BackofficeAuthenticationToken(userDetails, userDetails.getPassword(), userDetails.getAuthorities());
            }

            Collection<? extends GrantedAuthority> authorities = getAuthorities(map);
            return new UsernamePasswordAuthenticationToken(principal, "N/A", authorities);
        }
        return null;
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Map<String, ?> map) {
        if (!map.containsKey(AUTHORITIES)) {
            return null;
        }
        Object authorities = map.get(AUTHORITIES);
        if (authorities instanceof String) {
            return AuthorityUtils.commaSeparatedStringToAuthorityList((String) authorities);
        }
        if (authorities instanceof Collection) {
            return AuthorityUtils.commaSeparatedStringToAuthorityList(StringUtils
                    .collectionToCommaDelimitedString((Collection<?>) authorities));
        }
        throw new IllegalArgumentException("Authorities must be either a String or a Collection");
    }

}
