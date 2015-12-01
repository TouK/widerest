package pl.touk.widerest.multitenancy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { IdentifierToolTest.class, IdentifierTool.class })
@Configuration
public class IdentifierToolTest {

    @Resource
    private IdentifierTool identifierTool;

    @Test
    public void shouldVerifyGeneratedIdentifier() throws Exception {
        identifierTool.verifyIdentifier(identifierTool.generateIdentifier());

    }
}