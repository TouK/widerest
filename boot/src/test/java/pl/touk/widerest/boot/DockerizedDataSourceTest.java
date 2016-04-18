package pl.touk.widerest.boot;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DockerizedDataSourceTest {

    @Test
    public void testStartDatabseInContainer() throws Exception {
        DockerizedDataSource dockerizedDataSource = new DockerizedDataSource();
        dockerizedDataSource.startDatabseInContainer();
        try {
            assertTrue(dockerizedDataSource.getTargetDataSource().getConnection().isValid(1));
        } finally {
            dockerizedDataSource.destroyContainer();
        }
    }
}