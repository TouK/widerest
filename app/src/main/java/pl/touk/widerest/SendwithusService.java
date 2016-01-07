package pl.touk.widerest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sendwithus.SendWithUs;
import com.sendwithus.exception.SendWithUsException;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class SendwithusService {

    public void sendOrder(UriComponents uri, Map<String, Object> templateData) throws SendWithUsException, JsonProcessingException {

        uri = uri.expand(templateData);

        SendWithUs sendwithus = Optional.of(uri.getUserInfo()).map(SendWithUs::new).get();

        String templateId = uri.getQueryParams().getFirst("templateId");

        Map<String, Object> recipient = new HashMap<>();
        recipient.put("address", uri.getQueryParams().getFirst("to"));

        sendwithus.send(templateId, recipient, templateData);


    }




}
