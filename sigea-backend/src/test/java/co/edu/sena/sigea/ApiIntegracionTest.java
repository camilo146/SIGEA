package co.edu.sena.sigea;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test de integración: verifica que el contexto Spring carga, que Actuator
 * responde y que un endpoint protegido (dashboard) responde correctamente
 * con un usuario ADMINISTRADOR mockeado.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("actuator health responde 200")
    void actuatorHealthRespondeOk() throws Exception {
        mockMvc.perform(get("/actuator/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    @DisplayName("dashboard estadísticas requiere autenticación (4xx sin token)")
    void dashboardSinAuthRetornaNoAutorizado() throws Exception {
        mockMvc.perform(get("/dashboard/estadisticas").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError()); // 401 o 403 según configuración de seguridad
    }

    @Test
    @DisplayName("dashboard estadísticas retorna 200 y JSON con rol ADMINISTRADOR")
    void dashboardConAdminRetornaEstadisticas() throws Exception {
        mockMvc.perform(get("/dashboard/estadisticas")
                        .with(user("admin@sigea.local").roles("ADMINISTRADOR"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEquipos").exists())
                .andExpect(jsonPath("$.totalCategorias").exists())
                .andExpect(jsonPath("$.totalAmbientes").exists())
                .andExpect(jsonPath("$.totalUsuarios").exists())
                .andExpect(jsonPath("$.prestamosActivos").exists())
                .andExpect(jsonPath("$.mantenimientosEnCurso").exists())
                .andExpect(jsonPath("$.totalTransferencias").exists());
    }
}
