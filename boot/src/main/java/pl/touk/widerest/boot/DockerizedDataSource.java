package pl.touk.widerest.boot;

import com.google.common.collect.Lists;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificateException;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class DockerizedDataSource extends DelegatingDataSource {

    public static final String CONTAINER_NAME_PREFIX = "widerest-db-";

    protected DockerClient docker;

    protected String containerId;

    protected String dockerHost;

    protected int databasePort;

    @PostConstruct
    public void startDatabseInContainer() throws Exception {

        configureDockerClient();
        checkStaleContainers();
        createContainer();
        startContainer();
        waitForDatabaseStart();

        setTargetDataSource(
                DataSourceBuilder.create()
                        .username(getUsername())
                        .password(getPassword())
                        .url(getUrl())
                        .build()
        );
    }

    @PreDestroy
    public void destroyContainer() throws Exception {
        docker.stopContainer(containerId, 30);
        docker.removeContainer(containerId, true);
    }

    public String getUrl() {
        return "jdbc:postgresql://" + dockerHost + ":" + databasePort + "/";
    }

    public String getUsername() {
        return "postgres";
    }

    public String getPassword() {
        return null;
    }

    private void configureDockerClient() throws DockerCertificateException {
        DefaultDockerClient.Builder builder = DefaultDockerClient.fromEnv();
        dockerHost = Optional.ofNullable(builder.uri().getHost()).orElse("localhost");
        docker = builder.build();
    }

    private void checkStaleContainers() throws DockerException, InterruptedException {
        Long staleContainersCount = docker.listContainers(
                DockerClient.ListContainersParam.allContainers()
        ).stream()
                .flatMap(c -> c.names().stream())
                .filter(name -> name.startsWith("/" + CONTAINER_NAME_PREFIX))
                .peek(log::debug)
                .collect(Collectors.counting());
        ;
        log.warn("Found {} probably stale containers. Consider removing them.", staleContainersCount);
    }

    private void createContainer() throws DockerException, InterruptedException, IOException {

        this.databasePort = findFreePort();

        final Map<String, List<PortBinding>> portBindings = new HashMap<>();
        portBindings.put("5432/tcp", Lists.newArrayList(PortBinding.of("", databasePort)));
        final HostConfig hostConfig = HostConfig.builder().portBindings(portBindings).build();

        ContainerConfig containerConfig = ContainerConfig.builder().image("postgres:9.4").hostConfig(hostConfig).build();
        ContainerCreation containerCreation = docker.createContainer(containerConfig, CONTAINER_NAME_PREFIX + UUID.randomUUID());
        if (!CollectionUtils.isEmpty(containerCreation.getWarnings())) {
            containerCreation.getWarnings().forEach(DockerizedDataSource.log::warn);
        }
        this.containerId = containerCreation.id();

    }

    private int findFreePort() throws IOException {
        if ("localhost".equals(dockerHost)) {
            try (ServerSocket socket = new ServerSocket(0)) {
                return socket.getLocalPort();
            }
        } else {
            for (int i = 0; i < 100; i++) {
                int randomPort = RandomUtils.nextInt(32768, 49151);
                try (Socket ignored = new Socket(dockerHost, randomPort)) {
                } catch (ConnectException ex) {
                    return randomPort;
                }
            }
            throw new RuntimeException("No available port found");
        }
    }


    private void startContainer() throws IOException, DockerException, InterruptedException {

        log.info("Starting database container");
        docker.startContainer(this.containerId);

    }

    private void waitForDatabaseStart() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            try {
                if (DriverManager.getConnection(getUrl(), getUsername(), getPassword()).isValid(3)) {
                    log.info("Database container seems to have started");
                    break;
                }
            } catch (SQLException e) {
                Thread.sleep(3000);
            }
            log.info("Waiting for the container to start up...");
        }
        log.error("Database container has probably not started");

    }

}
