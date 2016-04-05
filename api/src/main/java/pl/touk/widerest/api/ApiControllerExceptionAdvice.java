package pl.touk.widerest.api;

import cz.jirutka.spring.exhandler.RestHandlerExceptionResolver;
import org.broadleafcommerce.core.checkout.service.exception.CheckoutException;
import org.broadleafcommerce.core.order.service.exception.UpdateCartException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@ControllerAdvice(annotations = { RestController.class })
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
                .addHandler(CheckoutException.class, new ErrorMessageRestExceptionHandlerWithDefaults(CheckoutException.class, HttpStatus.CONFLICT))
                .addHandler(Exception.class, new ErrorMessageRestExceptionHandlerWithDefaults(Exception.class, HttpStatus.INTERNAL_SERVER_ERROR))
                .addErrorMessageHandler(UpdateCartException.class, HttpStatus.CONFLICT)
                .build();
    }


}
