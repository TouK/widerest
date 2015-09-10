package pl.touk.widerest;

import com.google.common.collect.Lists;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.ServerSocket;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class DockerizedDataSource extends DelegatingDataSource {

    @Autowired
    protected DockerClient docker;

    protected String containerId;

    @PostConstruct
    public void createContainer() throws Exception {
        ContainerConfig containerConfig = ContainerConfig.builder().image("postgres:9.4").build();
        ContainerCreation containerCreation = docker.createContainer(containerConfig, "widerest-db-"+ UUID.randomUUID());
        if (!CollectionUtils.isEmpty(containerCreation.getWarnings())) {
            containerCreation.getWarnings().forEach(log::warn);
        }
        this.containerId = containerCreation.id();

        int randomPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            randomPort = socket.getLocalPort();
        }

        final Map<String, List<PortBinding>> portBindings = new HashMap<String, List<PortBinding>>();
        portBindings.put("5432/tcp", Lists.newArrayList(PortBinding.of("", randomPort)));
        final HostConfig hostConfig = HostConfig.builder().portBindings(portBindings).build();

        docker.startContainer(this.containerId, hostConfig);
        setTargetDataSource(DataSourceBuilder.create().username("postgres").url("jdbc:postgresql://localhost:" + randomPort + "/").build());

        while (true) {
            try (Connection connection = getTargetDataSource().getConnection()) {
                return;
            } catch (Exception e) {
                log.info("Waiting for the container start up...");
                Thread.sleep(5000);
            }
        }

    }

    @PreDestroy
    public void destroyContainer() throws Exception {
        docker.stopContainer(containerId, 10);
        docker.removeContainer(containerId);
    }


    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
    }

}
