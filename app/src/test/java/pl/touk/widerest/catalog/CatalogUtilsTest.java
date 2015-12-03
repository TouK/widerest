package pl.touk.widerest.catalog;

import org.junit.Test;
import org.springframework.security.authentication.BadCredentialsException;
import pl.touk.widerest.api.catalog.CatalogUtils;
import pl.touk.widerest.api.catalog.exceptions.DtoValidationException;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class CatalogUtilsTest {

    @Test
    public void shouldReturnValidIdForValidUrlTest() throws MalformedURLException {
        final String validUrl = "http://c4d524aa-75c5-48b0-8474-1be0a458bc1c.localhost:8080/v1/categories/2008";
        assertThat(CatalogUtils.getIdFromUrl(validUrl), equalTo(2008L));
    }

    @Test(expected = DtoValidationException.class)
    public void shouldThrowExceptionForValidUrlWithAnEndingSlashTest() throws MalformedURLException {
        final String validUrl = "http://c4d524aa-75c5-48b0-8474-1be0a458bc1c.localhost:8080/v1/categories/2008/";
        assertThat(CatalogUtils.getIdFromUrl(validUrl), equalTo(2008L));
    }


    @Test(expected = DtoValidationException.class)
    public void shouldThrowExceptionForInValidUrl1Test() throws MalformedURLException {
        final String validUrl = "http://c4d524aa-75c5-48b0-8474-1be0a458bc1c.localhost:8080";
        assertThat(CatalogUtils.getIdFromUrl(validUrl), equalTo(2008L));
    }

    @Test(expected = NumberFormatException.class)
    public void shouldThrowExceptionForInValidUrl2Test() throws MalformedURLException {
        final String validUrl = "http://c4d524aa-75c5-48b0-8474-1be0a458bc1c.localhost:8080/v1";
        assertThat(CatalogUtils.getIdFromUrl(validUrl), equalTo(2008L));
    }

    @Test
    public void shouldReturnEmptySublistForEmptyListTest() {
        assertThat(CatalogUtils.getSublistForOffset(Collections.emptyList(), 1, 2).size(), equalTo(0));
    }

    @Test
    public void shouldReturnValidSublistForValidList1Test() {
        List<Integer> inputList = Arrays.asList(1, 2, 3, 4, 5, 6);
        List<Integer> outputSublist = Arrays.asList(1, 2, 3, 4, 5, 6);
        assertThat(CatalogUtils.getSublistForOffset(inputList, 0, 0), equalTo(outputSublist));
    }

    @Test
    public void shouldReturnValidSublistForValidList2Test() {
        List<Integer> inputList = Arrays.asList(1, 2, 3, 4, 5, 6);
        List<Integer> outputSublist = Arrays.asList(3, 4, 5, 6);
        assertThat(CatalogUtils.getSublistForOffset(inputList, 2, 0), equalTo(outputSublist));
    }

    @Test
    public void shouldReturnValidSublistForValidList3Test() {
        List<Integer> inputList = Arrays.asList(1, 2, 3, 4, 5, 6);
        List<Integer> outputSublist = Arrays.asList(2, 3, 4);
        assertThat(CatalogUtils.getSublistForOffset(inputList, 1, 3), equalTo(outputSublist));
    }

    @Test
    public void shouldReturnValidSublistForValidList4Test() {
        List<Integer> inputList = Arrays.asList(1, 2, 3, 4, 5, 6);
        List<Integer> outputSublist = Arrays.asList(5, 6);
        assertThat(CatalogUtils.getSublistForOffset(inputList, 4, 4), equalTo(outputSublist));
    }

    @Test
    public void shouldReturnValidSublistForBoundaryConditions1Test() {
        List<Integer> inputList = Arrays.asList(1, 2, 3, 4, 5, 6);
        assertThat(CatalogUtils.getSublistForOffset(inputList, -1, 0), equalTo(inputList));
    }

    @Test
    public void shouldReturnValidSublistForBoundaryConditions2Test() {
        List<Integer> inputList = Arrays.asList(1, 2, 3, 4, 5, 6);
        List<Integer> outputSublist = Arrays.asList(6);
        assertThat(CatalogUtils.getSublistForOffset(inputList, 5, 3), equalTo(outputSublist));
    }

    @Test
    public void shouldReturnValidSublistForBoundaryConditions3Test() {
        List<Integer> inputList = Arrays.asList(1, 2, 3, 4, 5, 6);
        assertThat(CatalogUtils.getSublistForOffset(inputList, 6, 4), equalTo(Collections.emptyList()));
    }

    @Test
    public void shouldReturnValidSublistForBoundaryConditions4Test() {
        List<Integer> inputList = Arrays.asList(1, 2, 3, 4, 5, 6);
        assertThat(CatalogUtils.getSublistForOffset(inputList, 0, 20), equalTo(inputList));
    }


}
