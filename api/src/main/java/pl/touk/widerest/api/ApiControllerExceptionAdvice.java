package pl.touk.widerest.api;

import cz.jirutka.spring.exhandler.RestHandlerExceptionResolver;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@ControllerAdvice(annotations = { RestController.class })
public class ApiControllerExceptionAdvice {

    @Resource
    protected RestHandlerExceptionResolver restExceptionResolver;

    @ExceptionHandler(value = Exception.class)
    public ModelAndView sampleErrorHandler(HttpServletRequest request, HttpServletResponse response, Exception ex) throws Exception {

        return restExceptionResolver.resolveException(request, response, null, ex);

    }

}
