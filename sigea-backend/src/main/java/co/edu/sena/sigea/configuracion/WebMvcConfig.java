package co.edu.sena.sigea.configuracion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configura el servidor para servir archivos subidos (fotos de equipos).
 * GET /api/v1/uploads/nombreArchivo sirve el archivo desde sigea.uploads.path.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${sigea.uploads.path:./uploads}")
    private String uploadsPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + (uploadsPath.endsWith("/") ? uploadsPath : uploadsPath + "/"));
    }
}
