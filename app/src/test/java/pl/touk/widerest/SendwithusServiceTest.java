package pl.touk.widerest;

import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class SendwithusServiceTest {

    @InjectMocks
    private SendwithusService sendwithusService = new SendwithusService();

    @Ignore("for manual execution only")
    @Test
    public void testSendOrder() throws Exception {

        Map<String, Object> orderDtoMap = new HashMap<>();
        orderDtoMap.put("first_name", "TESTER");
        orderDtoMap.put("email_address", "test@touk.pl");

        UriComponents uriComponents = UriComponentsBuilder
                .fromUriString("sendwithus://test_c92a0fa5bee7da24847f78d8b4e3963451d00eb5@?templateId=tem_NrXPntKhwnFjDCCbL2E8Rc&to={email_address}").build();

        sendwithusService.sendOrder(uriComponents, orderDtoMap);

    }
}