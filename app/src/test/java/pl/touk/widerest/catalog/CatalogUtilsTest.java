package pl.touk.widerest.catalog;

import org.junit.Test;
import org.springframework.security.authentication.BadCredentialsException;
import pl.touk.widerest.api.catalog.CatalogUtils;
import pl.touk.widerest.api.catalog.exceptions.DtoValidationException;

import java.net.MalformedURLException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class CatalogUtilsTest {

    @Test
    public void shouldReturnValidIdForValidUrlTest() throws MalformedURLException {
        final String validUrl = "http://c4d524aa-75c5-48b0-8474-1be0a458bc1c.localhost:8080/v1/categories/2008";
        assertThat(CatalogUtils.getCategoryIdFromUrl(validUrl), equalTo(2008L));
    }

    @Test(expected = DtoValidationException.class)
    public void shouldThrowExceptionForValidUrlWithAnEndingSlashTest() throws MalformedURLException {
        final String validUrl = "http://c4d524aa-75c5-48b0-8474-1be0a458bc1c.localhost:8080/v1/categories/2008/";
        assertThat(CatalogUtils.getCategoryIdFromUrl(validUrl), equalTo(2008L));
    }


    @Test(expected = DtoValidationException.class)
    public void shouldThrowExceptionForInValidUrl1Test() throws MalformedURLException {
        final String validUrl = "http://c4d524aa-75c5-48b0-8474-1be0a458bc1c.localhost:8080";
        assertThat(CatalogUtils.getCategoryIdFromUrl(validUrl), equalTo(2008L));
    }

    @Test(expected = NumberFormatException.class)
    public void shouldThrowExceptionForInValidUrl2Test() throws MalformedURLException {
        final String validUrl = "http://c4d524aa-75c5-48b0-8474-1be0a458bc1c.localhost:8080/v1";
        assertThat(CatalogUtils.getCategoryIdFromUrl(validUrl), equalTo(2008L));
    }


}
