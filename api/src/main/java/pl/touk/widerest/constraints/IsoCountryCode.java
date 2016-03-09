package pl.touk.widerest.constraints;

import lombok.extern.slf4j.Slf4j;
import org.broadleafcommerce.common.i18n.service.ISOService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IsoCountryCode.Validator.class)
public @interface IsoCountryCode {

    String message() default "{pl.touk.widerest.constraint.IsoCountryCode.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Slf4j
    public class Validator implements ConstraintValidator<IsoCountryCode, String>{

        @Autowired
        private ISOService isoService;

        @Override
        public void initialize(IsoCountryCode constraintAnnotation) {}

        @Override
        public boolean isValid(String country, ConstraintValidatorContext context) {
            return isoService.findISOCountryByAlpha2Code(country) != null;
        }
    }
}
