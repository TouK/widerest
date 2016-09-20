package pl.touk.widerest;

import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import pl.touk.widerest.base.ApiTestBase;

@SpringApplicationConfiguration(classes = { BraodleafConfiguration.class, Application.class })
@WebIntegrationTest(randomPort = true)
public abstract class AbstractTest extends ApiTestBase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();
}
