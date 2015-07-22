import pl.touk.widerest.base.ApiTestBase;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
public class SampleTest extends ApiTestBase {

    @Test
    public void noop() throws InterruptedException {
        log.info("Web server is running on port {}", serverPort);
        Thread.sleep(5000);
    }
}
