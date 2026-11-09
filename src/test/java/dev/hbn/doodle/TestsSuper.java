package dev.hbn.doodle;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class TestsSuper {

    private static final String REALM = "doodle";
    private static final String TRUSTED_CLIENT = "doodle";
    private static final String UNTRUSTED_CLIENT = "admin-cli";
    private static final String CLIENT_SECRET = "doodle-secret";
    private static final String USERNAME = "doodle";
    private static final String PASSWORD = "doodle";
    private static final Network network = Network.newNetwork();
    private static final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17")
    )
            .withImagePullPolicy(PullPolicy.alwaysPull())
            .withDatabaseName("doodle")
            .withUsername("doodle")
            .withPassword("password")
            .withNetwork(network)
            .withNetworkAliases("pgdb");
    private static final GenericContainer<?> keycloak = new GenericContainer<>(
            DockerImageName.parse("quay.io/keycloak/keycloak:26.7.3"))
            .withImagePullPolicy(PullPolicy.alwaysPull())
            .withNetwork(network)
            .withEnv("KC_HOSTNAME_STRICT", "false")
            .withEnv("KC_HOSTNAME_STRICT_HTTPS", "false")
            .withEnv("KC_PROXY", "none")
            .withEnv("KC_HTTP_ENABLED", "true")
            .withEnv("KC_HEALTH_ENABLED", "true")
            .withExposedPorts(8080, 9000)
            .withCopyFileToContainer(
                    MountableFile.forHostPath("docker/keycloak/doodle-realm.json"),
                    "/opt/keycloak/data/import/doodle-realm.json")
            .dependsOn(postgres)
            .withCommand("start-dev", "--import-realm")
            .waitingFor(
                    Wait.forHttp("/health/ready")
                            .forPort(9000)
                            .forStatusCode(200)
                            .withStartupTimeout(Duration.ofMinutes(3)));

    static {
        Startables.deepStart(postgres, keycloak).join();
    }

    protected final ZoneId utc = ZoneId.of("UTC");
    protected final JdbcTemplate jdbcTemplate;

    protected TestsSuper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @DynamicPropertySource
    public static void containerProperties(DynamicPropertyRegistry registry) {
        String issuer = authServerUrl() + "/realms/" + REALM;
        registry.add("spring.security.oauth2.client.provider.keycloak.issuer-uri", () -> issuer);
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> issuer);
        registry.add("oauth2.public-realm-url", () -> issuer);
        registry.add("oauth2.private-realm-url", () -> issuer);
        registry.add("oauth2.health-url", () -> "http://" + keycloak.getHost() + ":"
                + keycloak.getMappedPort(9000) + "/health/ready");
        registry.add("oauth2.issuers[0]", () -> issuer);
        registry.add("oauth2.requires-secure-metadata", () -> "false");
        registry.add("spring.r2dbc.url", () -> r2dbcUrl("doodle"));
        registry.add("spring.r2dbc.username", () -> "doodle");
        registry.add("spring.r2dbc.password", () -> "password");
        registry.add("spring.flyway.url", () -> jdbcUrl("doodle"));
        registry.add("spring.flyway.user", () -> "doodle");
        registry.add("spring.flyway.password", () -> "password");
        registry.add("spring.datasource.url", () -> jdbcUrl("doodle"));
        registry.add("spring.datasource.username", () -> "doodle");
        registry.add("spring.datasource.password", () -> "password");
    }

    private static String accessToken(String realm, String clientId, String username, String password) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "password");
        form.put("client_id", clientId);
        if (TRUSTED_CLIENT.equals(clientId)) {
            form.put("client_secret", CLIENT_SECRET);
        }
        form.put("username", username);
        form.put("password", password);
        String body = form.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(authServerUrl() + "/realms/" + realm + "/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while requesting a token for client " + clientId, e);
        }

        if (response.statusCode() < 200 || response.statusCode() > 299) {
            throw new IllegalStateException(
                    "Token request for client " + clientId + " on realm " + realm + " failed with "
                            + response.statusCode() + ": " + response.body());
        }

        Matcher matcher = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"").matcher(response.body());
        if (!matcher.find()) {
            throw new IllegalStateException("Token response for client " + clientId + " did not include access_token.");
        }
        return matcher.group(1);
    }

    private static String authServerUrl() {
        return "http://localhost:" + keycloak.getMappedPort(8080);
    }

    private static String jdbcUrl(String database) {
        return "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/" + database;
    }

    private static String r2dbcUrl(String database) {
        return "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/" + database;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    protected Instant today(int hour) {
        return Instant.now().atZone(utc).toLocalDate().atStartOfDay(utc).toInstant().plusSeconds(hour * 3600L);
    }

    @BeforeEach
    void clearDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE meeting_participant, meetings, timeslots RESTART IDENTITY CASCADE");
    }

    protected String readerToken() {
        return accessToken(REALM, TRUSTED_CLIENT, USERNAME, PASSWORD);
    }

    protected String untrustedToken() {
        return accessToken(REALM, UNTRUSTED_CLIENT, USERNAME, PASSWORD);
    }

    /*private static String startupFailure(Throwable failure) {
        String postgresImage = "postgres:17";
        String keycloakImage = "quay.io/keycloak/keycloak:26.7.3";
        StringBuilder sb = new StringBuilder();
        sb.append("Failed to start the Postgres and Keycloak containers used by tests.").append('\n');
        sb.append("Postgres image: ").append(postgresImage).append(" (").append(localImage(postgresImage)).append(')').append('\n');
        sb.append("Keycloak image: ").append(keycloakImage).append(" (").append(localImage(keycloakImage)).append(')').append('\n');
        sb.append(causeChain(failure));
        return sb.toString();
    }

    private static String localImage(String image) {
        try {
            Process process = new ProcessBuilder("docker", "image", "inspect", "--format", "{{.Id}}", image)
                    .redirectErrorStream(true)
                    .start();
            String output;
            try (InputStream in = process.getInputStream()) {
                output = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
            return process.waitFor() == 0 && !output.isEmpty()
                    ? "present locally"
                    : "not present locally: " + output;
        } catch (Throwable error) {
            return "could not inspect: " + error.getClass().getSimpleName() + ": " + error.getMessage();
        }
    }*/

    /*private static String causeChain(Throwable failure) {
        Set<Throwable> seen = new LinkedHashSet<>();
        List<String> lines = new ArrayList<>();
        Throwable current = failure;
        while (current != null && seen.add(current)) {
            StackTraceElement[] trace = current.getStackTrace();
            StackTraceElement frame = trace.length == 0 ? null : trace[0];
            String where = frame == null ? "" : " (" + frame.getFileName() + ":" + frame.getLineNumber() + ")";
            lines.add(current.getClass().getName() + where + ": " + current.getMessage());
            current = current.getCause();
        }
        return String.join("\n", lines);
    }*/

}
