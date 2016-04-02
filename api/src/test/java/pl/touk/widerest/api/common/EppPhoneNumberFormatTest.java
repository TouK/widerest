package pl.touk.widerest.api.common;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.api.Assertions;
import org.broadleafcommerce.profile.core.domain.Phone;
import org.broadleafcommerce.profile.core.domain.PhoneImpl;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.FieldPosition;

import static junitparams.JUnitParamsRunner.$;

@RunWith(JUnitParamsRunner.class)
public class EppPhoneNumberFormatTest {

    EppPhoneNumberFormat phoneNumberFormat = new EppPhoneNumberFormat();

    @Test
    @Parameters(method = "testFormatData")
    public void testFormat(String countrCode, String phoneNumber, String extension, String eppNumber) throws Exception {

        Phone phone = new PhoneImpl();
        phone.setCountryCode(countrCode);
        phone.setPhoneNumber(phoneNumber);
        phone.setExtension(extension);

        StringBuffer buf = new StringBuffer(32);
        phoneNumberFormat.format(phone, buf, new FieldPosition(0));
        Assertions.assertThat(buf.toString()).isEqualTo(eppNumber);

    }

    @Test
    @Parameters(method = "testParseData")
    public void testParseObject(String countrCode, String phoneNumber, String extension, String eppNumber) throws Exception {

        Phone phone = (Phone) phoneNumberFormat.parseObject(eppNumber);
        Assertions.assertThat(phone.getCountryCode()).isEqualTo(countrCode);
        Assertions.assertThat(phone.getPhoneNumber()).isEqualTo(phoneNumber);
        Assertions.assertThat(phone.getExtension()).isEqualTo(extension);
    }

    public Object testFormatData() {
        return $(
                $("48", "601101101", "4321", "+48.601101101x4321"),
                $("48", "601101101", null, "+48.601101101"),
                $("48", "601101101", "", "+48.601101101"),
                $(null, "601101101", null, "601101101"),
                $(null, "601101101","4321","601101101x4321"),
                $("", "601101101","4321","601101101x4321"),
                $(null, "601101101","","601101101")
        );
    }

    public Object testParseData() {
        return $(
                $("48", "601101101", "4321", "+48.601101101x4321"),
                $("48", "601101101", null, "+48.601101101"),
                $(null, "601101101", null, "601101101"),
                $(null, "601101101","4321","601101101x4321")
        );
    }

}