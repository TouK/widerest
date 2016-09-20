package pl.touk.widerest.api;

import org.assertj.core.api.Condition;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

public class ApiTestHttpConditions {
    public static final Condition<HttpClientErrorException> httpNotFoundCondition = new Condition<HttpClientErrorException>() {
        @Override
        public boolean matches(HttpClientErrorException value) {
            return value.getStatusCode().equals(HttpStatus.NOT_FOUND);
        }
    };

    public static final Condition<HttpClientErrorException> httpConflictCondition = new Condition<HttpClientErrorException>() {
        @Override
        public boolean matches(HttpClientErrorException value) {
            return value.getStatusCode().equals(HttpStatus.CONFLICT);
        }
    };

    public static final Condition<HttpClientErrorException> httpUnprocessableEntityCondition = new Condition<HttpClientErrorException>() {
        @Override
        public boolean matches(HttpClientErrorException value) {
            return value.getStatusCode().equals(HttpStatus.UNPROCESSABLE_ENTITY);
        }
    };

    public static final Condition<HttpClientErrorException> http4xxStatusCode = new Condition<HttpClientErrorException>() {
        @Override
        public boolean matches(HttpClientErrorException value) {
            return value.getStatusCode().is4xxClientError();
        }
    };
}
