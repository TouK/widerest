package pl.touk.widerest.security.authentication;

import org.broadleafcommerce.openadmin.server.security.service.AdminUserDetails;
import org.broadleafcommerce.openadmin.server.security.service.AdminUserDetailsServiceImpl;
import org.broadleafcommerce.profile.core.service.CustomerUserDetails;
import org.broadleafcommerce.profile.core.service.UserDetailsServiceImpl;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.token.UserAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class UserAuthenticationConverterImpl implements UserAuthenticationConverter {

    public static final String ISS = "iss";
    public static final String SUB = "sub";

    public static final String BACKOFFICE_SUB_PREFIX = "backoffice";
    public static final String SITE_SUB_PREFIX = "site";
    public static final String WIDEREST_ISS = "widerest";
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
        String tenantIdentifier = getTenantIdentifier();
        claims.put(ISS, WIDEREST_ISS + DELIMITER + tenantIdentifier);
        if (principal instanceof AdminUserDetails) {
            claims.put(SUB, BACKOFFICE_SUB_PREFIX + DELIMITER + ((AdminUserDetails) principal).getUsername());
        } else if (principal instanceof CustomerUserDetails){
            claims.put(SUB, SITE_SUB_PREFIX + DELIMITER + ((CustomerUserDetails) principal).getUsername());
        }

        return claims;
    }

    @Transactional
    public Authentication extractAuthentication(Map<String, ?> claims) {

        String tenantIdentifier = getTenantIdentifier();

        boolean isValidForCurrentTenant = Optional.ofNullable((String) claims.get(ISS))
                .map(issuer -> issuer.equals(WIDEREST_ISS + DELIMITER + tenantIdentifier))
                .orElse(false);

        if (!isValidForCurrentTenant)
            throw new InvalidTokenException("Not valid for this tenant");

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


//        if (claims.containsKey(SUB)) {
//
//
//            String[] result = org.apache.commons.lang3.StringUtils.split(claims.get(ISS).toString(), "/");
//
//            Object principal = claims.get(SUB);
//            String issuer = String.valueOf(claims.get(ISS));
//
//            if (siteUserDetailsService != null && SITE_ISS.equals(issuer)) {
//                UserDetails userDetails = siteUserDetailsService.loadUserByUsername(String.valueOf(principal));
//                return new SiteAuthenticationToken(userDetails, userDetails.getPassword(), userDetails.getAuthorities());
//            }
//
//            if (backofficeUserDetailsService != null && BACKOFFICE_ISS.equals(issuer)) {
//                UserDetails userDetails = backofficeUserDetailsService.loadUserByUsername(String.valueOf(principal));
//                return new BackofficeAuthenticationToken(userDetails, userDetails.getPassword(), userDetails.getAuthorities());
//            }
//
//            Collection<? extends GrantedAuthority> authorities = getAuthorities(claims);
//            return new UsernamePasswordAuthenticationToken(principal, "N/A", authorities);
//        }
//        return null;
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

    private String getTenantIdentifier() {
        return em.unwrap(Session.class).getTenantIdentifier();
    }

}
