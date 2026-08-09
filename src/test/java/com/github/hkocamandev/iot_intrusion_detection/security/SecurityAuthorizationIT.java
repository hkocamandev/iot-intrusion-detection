package com.github.hkocamandev.iot_intrusion_detection.security;

import com.github.hkocamandev.iot_intrusion_detection.TestcontainersConfiguration;
import com.github.hkocamandev.iot_intrusion_detection.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the authorization matrix over the real filter chain with real tokens.
 * The admin account arrives via UserSeeder; the analyst is created through the public
 * registration endpoint, which also proves registration is reachable anonymously.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "app.ml.enabled=false",
        "app.llm.enabled=false",
        "app.security.seed.admin-username=it-admin",
        "app.security.seed.admin-password=it-admin-password"
})
class SecurityAuthorizationIT {

    private static final String ANALYST_USER = "it-analyst";
    private static final String ANALYST_PASSWORD = "it-analyst-password";

    @Autowired TestRestTemplate rest;
    @Autowired JwtProperties jwtProperties;

    private String adminToken;
    private String analystToken;

    @BeforeEach
    void signIn() {
        // Registration is anonymous. JUnit builds a fresh instance per method, so this runs
        // once per test: CREATED the first time, CONFLICT afterwards. Both are fine here —
        // the 201 body and the 409 are asserted by their own dedicated tests below.
        ResponseEntity<Map> registered = rest.postForEntity("/api/auth/register",
                json("{\"username\":\"" + ANALYST_USER + "\",\"password\":\"" + ANALYST_PASSWORD + "\"}"),
                Map.class);
        assertThat(registered.getStatusCode())
                .isIn(HttpStatus.CREATED, HttpStatus.CONFLICT);

        adminToken = login("it-admin", "it-admin-password");
        analystToken = login(ANALYST_USER, ANALYST_PASSWORD);
    }

    @Test
    void registrationCreatesAnAnalyst() {
        // Verifies the role of the account signIn() created, without depending on which
        // method ran first.
        ResponseEntity<Map> me = rest.exchange("/api/auth/me", HttpMethod.GET,
                new HttpEntity<>(bearer(analystToken)), Map.class);

        assertThat(me.getBody().get("role")).isEqualTo("ANALYST");
    }

    @Test
    void staticPagesAreAnonymous() {
        assertThat(rest.getForEntity("/", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rest.getForEntity("/login.html", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(rest.getForEntity("/index.html", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        // The /*.js permitAll glob is single-segment; these are the real script URLs the
        // dashboard requests, so a regression in that matcher breaks the page, not just a test.
        assertThat(rest.getForEntity("/auth.js", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(rest.getForEntity("/app.js", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(rest.getForEntity("/chat.js", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void alertsRequireAuthentication() {
        assertThat(get("/api/alerts", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void statsRequireAuthentication() {
        assertThat(get("/api/stats", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void analystCanReadAlertsAndStats() {
        assertThat(get("/api/alerts", analystToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/api/stats", analystToken).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void adminCanReadAlertsAndStats() {
        assertThat(get("/api/alerts", adminToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/api/stats", adminToken).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void analystIsForbiddenFromChat() {
        ResponseEntity<String> response = post("/api/chat", analystToken,
                "{\"question\":\"anything\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("forbidden");
    }

    @Test
    void analystIsForbiddenFromLlmOptions() {
        assertThat(get("/api/llm/options", analystToken).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminPassesAuthorizationOnChat() {
        // 503 is the LLM-disabled response, so reaching it proves authorization succeeded.
        assertThat(post("/api/chat", adminToken, "{\"question\":\"anything\"}").getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void adminCanReadLlmOptions() {
        assertThat(get("/api/llm/options", adminToken).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void anonymousChatIsUnauthorizedNotForbidden() {
        assertThat(post("/api/chat", null, "{\"question\":\"anything\"}").getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void anonymousLlmOptionsIsUnauthorizedNotForbidden() {
        assertThat(get("/api/llm/options", null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void anonymousMeIsRejectedBeforeReachingTheController() {
        // AuthController.me reads SecurityContextHolder directly and maps the ROLE_ authority to
        // a Role constant, which would throw on an anonymous authentication. The filter chain must
        // refuse the request first — 401, never 500.
        assertThat(get("/api/auth/me", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService expiring = new JwtService(jwtProperties.secret(), Duration.ofMinutes(-5),
                jwtProperties.issuer());
        String expired = expiring.issue("it-admin", Role.ADMIN).token();

        assertThat(get("/api/alerts", expired).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService forger = new JwtService("a-totally-different-secret-of-sufficient-length",
                Duration.ofHours(1), jwtProperties.issuer());
        String forged = forger.issue("it-admin", Role.ADMIN).token();

        assertThat(get("/api/alerts", forged).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void garbageTokenIsRejected() {
        assertThat(get("/api/alerts", "not-a-jwt").getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void wrongPasswordAndUnknownUserReturnTheSameBody() {
        ResponseEntity<String> wrongPassword = rest.postForEntity("/api/auth/login",
                json("{\"username\":\"" + ANALYST_USER + "\",\"password\":\"wrong-password\"}"),
                String.class);
        ResponseEntity<String> unknownUser = rest.postForEntity("/api/auth/login",
                json("{\"username\":\"nobody-at-all\",\"password\":\"wrong-password\"}"),
                String.class);

        assertThat(wrongPassword.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unknownUser.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unknownUser.getBody()).isEqualTo(wrongPassword.getBody());
    }

    @Test
    void meReportsTheAuthenticatedSession() {
        ResponseEntity<Map> response = rest.exchange("/api/auth/me", HttpMethod.GET,
                new HttpEntity<>(bearer(analystToken)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("username")).isEqualTo(ANALYST_USER);
        assertThat(response.getBody().get("role")).isEqualTo("ANALYST");
    }

    @Test
    void getOnLoginIsUnauthorized() {
        // permitAll on /api/auth/login and /api/auth/register is scoped to HttpMethod.POST;
        // deleting that scoping (matching any method) would leave this test green too, but
        // pins that GET does not silently inherit the permitAll.
        assertThat(get("/api/auth/login", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void errorEndpointIsReachableAnonymously() {
        // /error is permitAll so a real (authenticated) 500 is not masked as a 401. Its own
        // status depends on the error attributes present on this request, so only assert that
        // the filter chain did not reject it.
        ResponseEntity<String> response = get("/error", null);

        assertThat(response.getStatusCode()).isNotIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    void duplicateRegistrationIsRejected() {
        ResponseEntity<String> response = rest.postForEntity("/api/auth/register",
                json("{\"username\":\"" + ANALYST_USER + "\",\"password\":\"" + ANALYST_PASSWORD + "\"}"),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("username_taken");
    }

    private String login(String username, String password) {
        ResponseEntity<Map> response = rest.postForEntity("/api/auth/login",
                json("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"),
                Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) response.getBody().get("token");
    }

    private ResponseEntity<String> get(String path, String token) {
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(bearer(token)), String.class);
    }

    private ResponseEntity<String> post(String path, String token, String body) {
        HttpHeaders headers = bearer(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private static HttpEntity<String> json(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private static HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }
}
