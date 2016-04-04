package pl.touk.widerest.api;

import cz.jirutka.spring.exhandler.RestHandlerExceptionResolver;
import cz.jirutka.spring.exhandler.handlers.ErrorMessageRestExceptionHandler;
import cz.jirutka.spring.exhandler.messages.ErrorMessage;
import org.broadleafcommerce.core.checkout.service.exception.CheckoutException;
import org.broadleafcommerce.core.order.service.exception.UpdateCartException;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

@ControllerAdvice(basePackageClasses = ApiControllerExceptionAdvice.class)
public class ApiControllerExceptionAdvice {

    protected RestHandlerExceptionResolver restExceptionResolver = restExceptionResolver();

    @ExceptionHandler(value = Exception.class)
    public ModelAndView sampleErrorHandler(HttpServletRequest request, HttpServletResponse response, Exception ex) throws Exception {

        return restExceptionResolver.resolveException(request, response, null, ex);

    }

    protected RestHandlerExceptionResolver restExceptionResolver() {
        return RestHandlerExceptionResolver.builder()
//                .messageSource( httpErrorMessageSource() )
                .defaultContentType(MediaType.APPLICATION_JSON)
                .addHandler(CheckoutException.class, new ErrorMessageRestExceptionHandler<CheckoutException>(CheckoutException.class, HttpStatus.CONFLICT) {
                    @Override
                    public ErrorMessage createBody(CheckoutException ex, HttpServletRequest req) {
                        ErrorMessage errorMessage = super.createBody(ex, req);
                        errorMessage.setDetail(ex.getMessage());
                        return errorMessage;
                    }
                })
                .addHandler(Exception.class, new ErrorMessageRestExceptionHandler(Exception.class, HttpStatus.INTERNAL_SERVER_ERROR) {
                    @Override
                    public ResponseEntity handleException(Exception ex, HttpServletRequest req) {
                        logException(ex, req);

                        ErrorMessage body = createBody(ex, req);
                        HttpHeaders headers = createHeaders(ex, req);

                        HttpStatus status = Optional.ofNullable(AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class))
                                .map(ResponseStatus::value).orElse(getStatus());
                        body.setStatus(status);
                        if (StringUtils.isEmpty(body.getTitle())) body.setTitle(status.getReasonPhrase());

                        return new ResponseEntity<>(body, headers, status);
                    }

                })
                .addErrorMessageHandler(UpdateCartException.class, HttpStatus.CONFLICT)
                .build();
    }




}
