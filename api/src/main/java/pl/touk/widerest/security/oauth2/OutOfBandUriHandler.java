package pl.touk.widerest.security.oauth2;

import java.util.stream.Collectors;

import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ModelAndViewMethodReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

public class OutOfBandUriHandler implements HandlerMethodReturnValueHandler {

    public static final String OOB_URI = "urn:ietf:wg:oauth:2.0:oob";

    protected HandlerMethodReturnValueHandler delegate;

    public OutOfBandUriHandler(ExceptionHandlerExceptionResolver exceptionHandlerExceptionResolver) {
        exceptionHandlerExceptionResolver.setReturnValueHandlers(
                exceptionHandlerExceptionResolver.getReturnValueHandlers().getHandlers().stream().map(handler -> {
                    if (handler instanceof ModelAndViewMethodReturnValueHandler) {
                        this.delegate = handler;
                        return this;
                    } else {
                        return handler;
                    }

                }).collect(Collectors.toList()));




    }

    public OutOfBandUriHandler(RequestMappingHandlerAdapter requestMappingHandlerAdapter) {
        requestMappingHandlerAdapter.setReturnValueHandlers(
                requestMappingHandlerAdapter.getReturnValueHandlers().stream().map(handler -> {
                    if (handler instanceof ModelAndViewMethodReturnValueHandler) {
                        this.delegate = handler;
                        return this;
                    } else {
                        return handler;
                    }

                }).collect(Collectors.toList()));
    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        return delegate.supportsReturnType(returnType);
    }

    @Override
    public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
        ModelAndView mav = (ModelAndView) returnValue;
        View view = mav.getView();
        if (view instanceof RedirectView) {
            String location = ((RedirectView) view).getUrl();
            if (location.startsWith(OOB_URI)) {
                UriComponents uriComponents = UriComponentsBuilder.newInstance().query(location.substring(OOB_URI.length() + 1)).build();
                mavContainer.setView(new MappingJackson2JsonView());
                mavContainer.addAllAttributes(uriComponents.getQueryParams().toSingleValueMap());
                return;
            }
        }
        delegate.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
    }


}
