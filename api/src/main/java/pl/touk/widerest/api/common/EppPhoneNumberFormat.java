package pl.touk.widerest.api.common;

import org.broadleafcommerce.profile.core.domain.Phone;
import org.broadleafcommerce.profile.core.domain.PhoneImpl;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EppPhoneNumberFormat extends Format {

    @Override
    public StringBuffer format(Object obj, StringBuffer buf, FieldPosition pos) {
        Phone number = (Phone) obj;
        Optional.ofNullable(number.getCountryCode())
                .filter(((Predicate<String>) String::isEmpty).negate())
                .ifPresent(ccc -> buf.append('+').append(ccc).append('.'));
        Optional.ofNullable(number.getPhoneNumber())
                .ifPresent(nnnnnnn -> buf.append(nnnnnnn));
        Optional.ofNullable(number.getExtension())
                .filter(((Predicate<String>) String::isEmpty).negate())
                .ifPresent(eeee -> buf.append('x').append(eeee));
        return buf;
    }

    @Override
    public Phone parseObject(String source, ParsePosition pos) {
        Pattern pattern = Pattern.compile("^(?:\\+([0-9]{1,3}))?\\.?([0-9]{4,14})x?(.+)?$");
        Matcher matcher = pattern.matcher(source);
        if (matcher.find(pos.getIndex())) {
            Phone phone = new PhoneImpl();
            phone.setCountryCode(matcher.group(1));
            phone.setPhoneNumber(matcher.group(2));
            phone.setExtension(matcher.group(3));
            pos.setIndex(matcher.end());
            return phone;
        } else {
            return null;
        }
    }

}
