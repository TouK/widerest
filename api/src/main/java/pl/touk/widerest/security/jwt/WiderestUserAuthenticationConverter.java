package pl.touk.widerest.security.jwt;

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
import pl.touk.widerest.security.authentication.BackofficeAuthenticationToken;
import pl.touk.widerest.security.authentication.SiteAuthenticationToken;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class WiderestUserAuthenticationConverter implements UserAuthenticationConverter {

    public static final String SUB = "sub";

    public static final String BACKOFFICE_SUB_PREFIX = "backoffice";
    public static final String SITE_SUB_PREFIX = "site";
    public static final String DELIMITER = "/";

    @Autowired
    protected AdminUserDetailsServiceImpl backofficeUserDetailsService;

    @Autowired
    protected UserDetailsServiceImpl siteUserDetailsService;

    @PersistenceContext(unitName = "blPU")
    protected EntityManager em;

    public Map<String, ?> convertUserAuthentication(Authentication authentication) {
        Map<String, Object> claims = new LinkedHashMap<String, Object>();
        Object principal = authentication.getPrincipal();
        if (principal instanceof AdminUserDetails) {
            claims.put(SUB, BACKOFFICE_SUB_PREFIX + DELIMITER + ((AdminUserDetails) principal).getUsername());
        } else if (principal instanceof CustomerUserDetails){
            claims.put(SUB, SITE_SUB_PREFIX + DELIMITER + ((CustomerUserDetails) principal).getUsername());
        }

        return claims;
    }

    @Transactional
    public Authentication extractAuthentication(Map<String, ?> claims) {

        return Optional.ofNullable(claims.get(SUB))
                .map(String.class::cast)
                .map(subject -> StringUtils.split(subject, DELIMITER))
                .map(subject ->
                        {
                            if (siteUserDetailsService != null && SITE_SUB_PREFIX.equals(subject[0])) {
                                UserDetails userDetails = siteUserDetailsService.loadUserByUsername(String.valueOf(subject[1]));
                                return new SiteAuthenticationToken(userDetails, userDetails.getPassword(), userDetails.getAuthorities());
                            }

                            if (backofficeUserDetailsService != null && BACKOFFICE_SUB_PREFIX.equals(subject[0])) {
                                UserDetails userDetails = backofficeUserDetailsService.loadUserByUsername(String.valueOf(subject[1]));
                                return new BackofficeAuthenticationToken(userDetails, userDetails.getPassword(), userDetails.getAuthorities());
                            }

                            Collection<? extends GrantedAuthority> authorities = getAuthorities(claims);
                            return new UsernamePasswordAuthenticationToken(subject[1], "N/A", authorities);
                        }
                ).orElse(null);


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
