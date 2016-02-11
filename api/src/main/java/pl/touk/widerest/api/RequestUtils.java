package pl.touk.widerest.api;

import java.util.Optional;

import org.broadleafcommerce.common.web.BroadleafRequestContext;
import org.springframework.web.context.request.RequestAttributes;

public class RequestUtils {

    public static final String CHANNEL_REQUEST_ATTRIBUTE_NAME = "x-channel";

    public static String getRequestChannel() {
        return getAttribute(CHANNEL_REQUEST_ATTRIBUTE_NAME, null);
    }

    public static void setRequestChannel(String channel) {
        setAttribute(CHANNEL_REQUEST_ATTRIBUTE_NAME, channel);
    }

    private static <T> T getAttribute(String key, T defaultValue) {
        return Optional.ofNullable(BroadleafRequestContext.getBroadleafRequestContext())
                .map(BroadleafRequestContext::getWebRequest)
                .map(request -> (T) request.getAttribute(key, RequestAttributes.SCOPE_REQUEST))
                .orElse(defaultValue);
    }

    private static <T> void setAttribute(String key, T value) {
        Optional.ofNullable(BroadleafRequestContext.getBroadleafRequestContext())
                .map(BroadleafRequestContext::getWebRequest)
                .ifPresent(request -> request.setAttribute(key, value, RequestAttributes.SCOPE_REQUEST));
    }
}
