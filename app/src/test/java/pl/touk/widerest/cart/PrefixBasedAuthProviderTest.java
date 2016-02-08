package pl.touk.widerest.cart;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.springframework.security.authentication.BadCredentialsException;
import pl.touk.widerest.security.authentication.PrefixBasedAuthenticationManager;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class PrefixBasedAuthProviderTest {

    private Pair<String, String> resultsPair;

    @Test
    public void passingProperDataParsesCorrectlyTest() {
        resultsPair = PrefixBasedAuthenticationManager.getAuthDataFromString("site/admin");
        assertThat(resultsPair.getLeft(), equalTo("site"));
        assertThat(resultsPair.getRight(), equalTo("admin"));
    }


    @Test(expected = BadCredentialsException.class)
    public void emptyInputStringCausesExceptionTest() {
        resultsPair = PrefixBasedAuthenticationManager.getAuthDataFromString("");
    }

    @Test(expected = BadCredentialsException.class)
     public void nullInputStringCasesExceptionTest() {
        resultsPair = PrefixBasedAuthenticationManager.getAuthDataFromString(null);
     }

    @Test(expected = BadCredentialsException.class)
    public void passingTooManyStringsCausesExceptionTest() {
        resultsPair = PrefixBasedAuthenticationManager.getAuthDataFromString("site/admin/site");
    }

    @Test(expected = BadCredentialsException.class)
    public void passingTooManyStringsCausesException2Test() {
        resultsPair = PrefixBasedAuthenticationManager.getAuthDataFromString("admin//site/a//dmin/s/ite");
    }

    @Test(expected = BadCredentialsException.class)
    public void passingSingleStringCausesExceptionTest() {
        resultsPair = PrefixBasedAuthenticationManager.getAuthDataFromString("site");
    }

    @Test(expected = BadCredentialsException.class)
    public void passingSingleStringFollowedBySlashCausesExceptionTest() {
        resultsPair = PrefixBasedAuthenticationManager.getAuthDataFromString("site/");
    }

    @Test
    public void leadingSlashFollowedByProperDataParsesCorrectlyTest() {
        resultsPair = PrefixBasedAuthenticationManager.getAuthDataFromString("/site/admin");
        assertThat(resultsPair.getLeft(), equalTo("site"));
        assertThat(resultsPair.getRight(), equalTo("admin"));
    }

    @Test
    public void properDataFollowedWithASlashParsesCorrectlyTest() {
        resultsPair = PrefixBasedAuthenticationManager.getAuthDataFromString("site/admin/");
        assertThat(resultsPair.getLeft(), equalTo("site"));
        assertThat(resultsPair.getRight(), equalTo("admin"));
    }

}
