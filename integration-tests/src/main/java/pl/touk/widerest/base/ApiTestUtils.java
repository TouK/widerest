package pl.touk.widerest.base;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class ApiTestUtils {

    public static Date addNDaysToDate(final Date date, final int N) {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, N);
        return cal.getTime();
    }

    public static int strapSuffixId(final String url) {
        // Assuming it is */df/ab/{sufix}
        final String[] tab = StringUtils.split(url, "/");
        return Integer.parseInt(tab[tab.length - 1]);
    }

    public static long getIdFromLocationUrl(final String locationUrl) {
        if(locationUrl != null && org.apache.commons.lang.StringUtils.isNotEmpty(locationUrl)) {
            return Long.parseLong(locationUrl.substring(locationUrl.lastIndexOf('/') + 1));
        } else {
            return -1;
        }

    }

    public static long getIdFromEntity(final ResponseEntity responseEntity) {
        return getIdFromLocationUrl(responseEntity.getHeaders().getLocation().toASCIIString());
    }

    public static String getAccessTokenFromLocationUrl(final String locationUrl) throws URISyntaxException {
        final String accessTokenUrl = locationUrl.replace("#", "?");
        final List<NameValuePair> authorizationParams = URLEncodedUtils.parse(new URI(accessTokenUrl), "UTF-8");

        return authorizationParams.stream()
                .filter(x -> x.getName().equals("access_token"))
                .collect(Collectors.toList()).get(0).getValue();
    }

    public static String getAccessTokenFromEntity(final ResponseEntity responseEntity) throws URISyntaxException {
        return getAccessTokenFromLocationUrl(responseEntity.getHeaders().getLocation().toASCIIString());
    }

    public static String strapTokenFromURI(final URI response) throws URISyntaxException {
        final String authorizationUrl = response.toString().replaceFirst("#", "?");
        final List<NameValuePair> authParams = URLEncodedUtils.parse(new URI(authorizationUrl), "UTF-8");

        return authParams.stream()
                .filter(x -> x.getName().equals("access_token"))
                .findFirst()
                .map(NameValuePair::getValue)
                .orElse(null);
    }
}
