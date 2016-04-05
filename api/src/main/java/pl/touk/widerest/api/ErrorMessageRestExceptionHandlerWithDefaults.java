package pl.touk.widerest.api;

import cz.jirutka.spring.exhandler.handlers.ErrorMessageRestExceptionHandler;
import cz.jirutka.spring.exhandler.messages.ErrorMessage;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public class ErrorMessageRestExceptionHandlerWithDefaults<E extends Exception> extends ErrorMessageRestExceptionHandler<E> {
    public ErrorMessageRestExceptionHandlerWithDefaults(Class<E> exceptionClass, HttpStatus status) {
        super(exceptionClass, status);
    }

    @Override
    public ResponseEntity handleException(E ex, HttpServletRequest req) {
        logException(ex, req);

        ErrorMessage body = createBody(ex, req);
        HttpHeaders headers = createHeaders(ex, req);

        HttpStatus status = Optional.ofNullable(AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class))
                .map(ResponseStatus::value).orElse(getStatus());
        body.setStatus(status);
        if (StringUtils.isEmpty(body.getTitle())) body.setTitle(status.getReasonPhrase());
        if (StringUtils.isEmpty(body.getDetail())) body.setDetail(ex.getLocalizedMessage());

        return new ResponseEntity<>(body, headers, status);
    }

}
