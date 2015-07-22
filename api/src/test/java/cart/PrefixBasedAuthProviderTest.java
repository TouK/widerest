package cart;


import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.authentication.AuthenticationManagerBeanDefinitionParser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import pl.touk.widerest.security.authentication.CustomAuthenticationProvider;
import pl.touk.widerest.security.authentication.PrefixBasedAuthenticationProvider;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;

/**
 * Created by mst on 21.07.15.
 */
public class PrefixBasedAuthProviderTest {

    @Mock
    private PrefixBasedAuthenticationProvider prefixBasedAuthenticationProvider;
    @InjectMocks
    private Map<String, AuthenticationProvider> authenticationProviders;

    private Authentication authentication;

    @Before
    public void initPrefixBasedAuthProviderTest() {

        authenticationProviders = new HashMap<>();

        

        //provider.addProvider("site", siteAuthenticationProvider());
        //provider.addProvider("backoffice", backofficeAuthenticationProvider());

        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void simpleTest() {
        prefixBasedAuthenticationProvider.authenticate(authentication);
    }

}
