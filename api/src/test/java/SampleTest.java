import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import pl.touk.widerest.Application;
import pl.touk.widerest.BroadleafApplicationContextInitializer;

@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
public class SampleTest extends ApiTestBase {

    @Test
    public void noop() throws InterruptedException {
        log.info("Web server is running on port {}", serverPort);
        Thread.sleep(5000);
    }
}
