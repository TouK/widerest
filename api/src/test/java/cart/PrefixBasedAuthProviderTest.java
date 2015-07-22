package cart;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import pl.touk.widerest.security.authentication.PrefixBasedAuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by mst on 21.07.15.
 */
public class PrefixBasedAuthProviderTest {

    private Pair<String, String> resultsPair;

    @Test
    public void passingProperDataParsesCorrectlyTest() {
        resultsPair = PrefixBasedAuthenticationProvider.getAuthDataFromString("site/admin");
        assertThat(resultsPair.getLeft(), equalTo("site"));
        assertThat(resultsPair.getRight(), equalTo("admin"));
    }


    @Test(expected = BadCredentialsException.class)
    public void emptyInputStringCausesExceptionTest() {
        resultsPair = PrefixBasedAuthenticationProvider.getAuthDataFromString("");
    }

    @Test(expected = BadCredentialsException.class)
     public void nullInputStringCasesExceptionTest() {
        resultsPair = PrefixBasedAuthenticationProvider.getAuthDataFromString(null);
     }

    @Test(expected = BadCredentialsException.class)
    public void passingTooManyStringsCausesExceptionTest() {
        resultsPair = PrefixBasedAuthenticationProvider.getAuthDataFromString("site/admin/site");
    }

    @Test(expected = BadCredentialsException.class)
    public void passingTooManyStringsCausesException2Test() {
        resultsPair = PrefixBasedAuthenticationProvider.getAuthDataFromString("admin//site/a//dmin/s/ite");
    }

    @Test(expected = BadCredentialsException.class)
    public void passingSingleStringCausesExceptionTest() {
        resultsPair = PrefixBasedAuthenticationProvider.getAuthDataFromString("site");
    }

    @Test(expected = BadCredentialsException.class)
    public void passingSingleStringFollowedBySlashCausesExceptionTest() {
        resultsPair = PrefixBasedAuthenticationProvider.getAuthDataFromString("site/");
    }

    @Test
    public void leadingSlashFollowedByProperDataParsesCorrectlyTest() {
        resultsPair = PrefixBasedAuthenticationProvider.getAuthDataFromString("/site/admin");
        assertThat(resultsPair.getLeft(), equalTo("site"));
        assertThat(resultsPair.getRight(), equalTo("admin"));
    }

    @Test
    public void followingSlashCausesExceptionTest() {
        resultsPair = PrefixBasedAuthenticationProvider.getAuthDataFromString("site/admin/");
        assertThat(resultsPair.getLeft(), equalTo("site"));
        assertThat(resultsPair.getRight(), equalTo("admin"));
    }

}
