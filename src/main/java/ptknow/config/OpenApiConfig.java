package ptknow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI ptKnowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("PTKnow API")
                        .version("v1.0.0")
                        .description("REST API платформы PTKnow. Access token передаётся в заголовке Authorization Bearer. Refresh token хранится в HttpOnly-cookie и используется только на /api/v1/token/refresh.")
                        .contact(new Contact().name("PTKnow")))
                .tags(List.of(
                        new Tag().name("Аутентификация").description("Регистрация, вход, выход и обновление access token."),
                        new Tag().name("Профиль").description("Получение и изменение профиля пользователя."),
                        new Tag().name("Курсы").description("Каталог курсов, управление курсами, преподавателями и участниками."),
                        new Tag().name("Уроки").description("CRUD уроков, markdown-содержимое и материалы уроков."),
                        new Tag().name("Файлы").description("Скачивание, метаданные и удаление файлов."),
                        new Tag().name("Администрирование пользователей").description("Методы управления пользователями, доступные только ADMIN."),
                        new Tag().name("Служебные точки").description("Точки проверки состояния приложения и базовая служебная информация.")
                ))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
