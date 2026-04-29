package com.ganaderia4.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganaderia4.backend.dto.LoginRequestDTO;
import com.ganaderia4.backend.model.Role;
import com.ganaderia4.backend.model.User;
import com.ganaderia4.backend.repository.UserRepository;
import com.ganaderia4.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecurityAuthorizationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        createUser("Administrador", "admin@test.com", "12345678", Role.ADMINISTRADOR, true);
        createUser("Operador", "operador@test.com", "12345678", Role.OPERADOR, true);
        createUser("Supervisor", "supervisor@test.com", "12345678", Role.SUPERVISOR, true);
        createUser("Tecnico", "tecnico@test.com", "12345678", Role.TECNICO, true);
    }

    @Test
    void shouldReturnCurrentUserWhenTokenIsValid() throws Exception {
        String token = loginAndGetToken("admin@test.com", "12345678");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@test.com"))
                .andExpect(jsonPath("$.role").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldRejectAuthMeWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("No autorizado"))
                .andExpect(jsonPath("$.path").value("/api/auth/me"));
    }

    @Test
    void shouldAllowHealthzWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/healthz"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void shouldKeepApiHealthProtected() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("No autorizado"))
                .andExpect(jsonPath("$.path").value("/api/health"));
    }

    @Test
    void shouldAllowAdminToGetUsers() throws Exception {
        String token = loginAndGetToken("admin@test.com", "12345678");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    void shouldAllowAdminToGetUsersPage() throws Exception {
        String token = loginAndGetToken("admin@test.com", "12345678");

        mockMvc.perform(get("/api/users/page")
                        .param("size", "2")
                        .param("sort", "id")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    void shouldRejectUsersPageWhenSizeExceedsGlobalMaximum() throws Exception {
        String token = loginAndGetToken("admin@test.com", "12345678");

        mockMvc.perform(get("/api/users/page")
                        .param("size", "101")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("El tamano de pagina debe estar entre 1 y 100"))
                .andExpect(jsonPath("$.path").value("/api/users/page"));
    }

    @Test
    void shouldRejectUsersPageWhenDirectionIsInvalid() throws Exception {
        String token = loginAndGetToken("admin@test.com", "12345678");

        mockMvc.perform(get("/api/users/page")
                        .param("direction", "DOWN")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("La direccion de ordenamiento debe ser ASC o DESC"))
                .andExpect(jsonPath("$.path").value("/api/users/page"));
    }

    @Test
    void shouldDenyOperatorAccessToUsers() throws Exception {
        String token = loginAndGetToken("operador@test.com", "12345678");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Acceso denegado"))
                .andExpect(jsonPath("$.path").value("/api/users"));
    }

    @Test
    void shouldAllowOperatorToGetCows() throws Exception {
        String token = loginAndGetToken("operador@test.com", "12345678");

        mockMvc.perform(get("/api/cows")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldDenySupervisorFromResolvingAlerts() throws Exception {
        String token = loginAndGetToken("supervisor@test.com", "12345678");

        mockMvc.perform(patch("/api/alerts/1/resolve")
                        .param("observations", "no autorizado")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Acceso denegado"))
                .andExpect(jsonPath("$.path").value("/api/alerts/1/resolve"));
    }

    @Test
    void shouldDenySupervisorAccessToUsersPage() throws Exception {
        String token = loginAndGetToken("supervisor@test.com", "12345678");

        mockMvc.perform(get("/api/users/page")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/api/users/page"));
    }

    @Test
    void shouldDenyTecnicoAccessToUsersPage() throws Exception {
        String token = loginAndGetToken("tecnico@test.com", "12345678");

        mockMvc.perform(get("/api/users/page")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/api/users/page"));
    }

    @Test
    void shouldDenyOperadorAccessToUsersPage() throws Exception {
        String token = loginAndGetToken("operador@test.com", "12345678");

        mockMvc.perform(get("/api/users/page")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/api/users/page"));
    }

    @Test
    void shouldRejectUsersPageWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/users/page"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/users/page"));
    }

    @Test
    void shouldAllowAdminToGetAuditLogs() throws Exception {
        String token = loginAndGetToken("admin@test.com", "12345678");

        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldDenySupervisorAccessToAuditLogs() throws Exception {
        assertForbiddenForRole("/api/audit-logs", loginAndGetToken("supervisor@test.com", "12345678"));
    }

    @Test
    void shouldDenyTecnicoAccessToAuditLogs() throws Exception {
        assertForbiddenForRole("/api/audit-logs", loginAndGetToken("tecnico@test.com", "12345678"));
    }

    @Test
    void shouldDenyOperadorAccessToAuditLogs() throws Exception {
        assertForbiddenForRole("/api/audit-logs", loginAndGetToken("operador@test.com", "12345678"));
    }

    @Test
    void shouldRejectAuditLogsWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/audit-logs"));
    }

    @Test
    void shouldAllowAdminToGetGeofencesPage() throws Exception {
        assertAllowedStatus(get("/api/geofences/page")
                        .param("sort", "id"),
                loginAndGetToken("admin@test.com", "12345678"),
                200);
    }

    @Test
    void shouldAllowSupervisorToGetGeofencesPage() throws Exception {
        assertAllowedStatus(get("/api/geofences/page")
                        .param("sort", "id"),
                loginAndGetToken("supervisor@test.com", "12345678"),
                200);
    }

    @Test
    void shouldDenyTecnicoAccessToGeofencesPage() throws Exception {
        mockMvc.perform(get("/api/geofences/page")
                        .param("sort", "id")
                        .header("Authorization", "Bearer " + loginAndGetToken("tecnico@test.com", "12345678")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/api/geofences/page"));
    }

    @Test
    void shouldDenyOperadorAccessToGeofencesPage() throws Exception {
        mockMvc.perform(get("/api/geofences/page")
                        .param("sort", "id")
                        .header("Authorization", "Bearer " + loginAndGetToken("operador@test.com", "12345678")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/api/geofences/page"));
    }

    @Test
    void shouldRejectGeofencesPageWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/geofences/page")
                        .param("sort", "id"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/geofences/page"));
    }

    @Test
    void shouldAllowAdminToGetCowLocationHistoryEndpoint() throws Exception {
        assertAllowedStatus(get("/api/locations/cow/999/last"),
                loginAndGetToken("admin@test.com", "12345678"),
                200, 404);
    }

    @Test
    void shouldAllowSupervisorToGetCowLocationHistoryEndpoint() throws Exception {
        assertAllowedStatus(get("/api/locations/cow/999/last"),
                loginAndGetToken("supervisor@test.com", "12345678"),
                200, 404);
    }

    @Test
    void shouldAllowTecnicoToGetCowLocationHistoryEndpoint() throws Exception {
        assertAllowedStatus(get("/api/locations/cow/999/last"),
                loginAndGetToken("tecnico@test.com", "12345678"),
                200, 404);
    }

    @Test
    void shouldAllowOperadorToGetCowLocationHistoryEndpoint() throws Exception {
        assertAllowedStatus(get("/api/locations/cow/999/last"),
                loginAndGetToken("operador@test.com", "12345678"),
                200, 404);
    }

    @Test
    void shouldRejectCowLocationHistoryEndpointWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/locations/cow/999/last"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/locations/cow/999/last"));
    }

    @Test
    void shouldAllowAdminToGetDashboardSummary() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + loginAndGetToken("admin@test.com", "12345678")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldAllowSupervisorToGetDashboardSummary() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + loginAndGetToken("supervisor@test.com", "12345678")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldAllowTecnicoToGetDashboardSummary() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + loginAndGetToken("tecnico@test.com", "12345678")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldAllowOperadorToGetDashboardSummary() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + loginAndGetToken("operador@test.com", "12345678")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldRejectDashboardSummaryWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/dashboard/summary"));
    }

    @Test
    void shouldAllowAdminToGetCowsPage() throws Exception {
        mockMvc.perform(get("/api/cows/page")
                        .param("sort", "id")
                        .header("Authorization", "Bearer " + loginAndGetToken("admin@test.com", "12345678")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldAllowSupervisorToGetCowsPage() throws Exception {
        mockMvc.perform(get("/api/cows/page")
                        .param("sort", "id")
                        .header("Authorization", "Bearer " + loginAndGetToken("supervisor@test.com", "12345678")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldAllowOperadorToGetCowsPage() throws Exception {
        mockMvc.perform(get("/api/cows/page")
                        .param("sort", "id")
                        .header("Authorization", "Bearer " + loginAndGetToken("operador@test.com", "12345678")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldDenyTecnicoAccessToCowsPage() throws Exception {
        mockMvc.perform(get("/api/cows/page")
                        .param("sort", "id")
                        .header("Authorization", "Bearer " + loginAndGetToken("tecnico@test.com", "12345678")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/api/cows/page"));
    }

    @Test
    void shouldRejectCowsPageWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/cows/page")
                        .param("sort", "id"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/cows/page"));
    }

    private void assertForbiddenForRole(String path, String token) throws Exception {
        mockMvc.perform(get(path)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.path").value(path));
    }

    private void assertAllowedStatus(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                                     String token,
                                     int... expectedStatuses) throws Exception {
        ResultActions resultActions = mockMvc.perform(request.header("Authorization", "Bearer " + token));
        int actualStatus = resultActions.andReturn().getResponse().getStatus();

        for (int expectedStatus : expectedStatuses) {
            if (actualStatus == expectedStatus) {
                return;
            }
        }

        assertTrue(false, "Expected one of the statuses " + java.util.Arrays.toString(expectedStatuses)
                + " but got " + actualStatus + " for " + request.buildRequest(mockMvc.getDispatcherServlet().getServletContext()).getRequestURI());
    }

    private void createUser(String name, String email, String rawPassword, Role role, boolean active) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setActive(active);
        userRepository.save(user);
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail(email);
        request.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }
}
