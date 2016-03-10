package pl.touk.widerest.boot;

import org.junit.Test;

import java.sql.Connection;

import static org.junit.Assert.*;

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