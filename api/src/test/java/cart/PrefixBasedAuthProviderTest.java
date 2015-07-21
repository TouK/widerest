package cart;


import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pl.touk.widerest.security.authentication.PrefixBasedAuthenticationProvider;

/**
 * Created by mst on 21.07.15.
 */
public class PrefixBasedAuthProviderTest {

    @Mock
    private PrefixBasedAuthenticationProvider prefixBasedAuthenticationProvider;

    @Before
    public void initPrefixBasedAuthProviderTest() {

        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void simpleTest() {
        System.out.println("BOOM!");
    }

}
