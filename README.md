# PTKnowBackend

PTKnowBackend — backend платформы дополнительного образования.

## Что делает проект

Backend предоставляет REST API для работы с образовательной платформой:
- регистрация, вход, refresh/logout и JWT-аутентификация
- профили пользователей
- курсы, уроки и markdown-контент уроков
- запись на курсы (enrollment)
- роли и модель владения ресурсами
- загрузка и выдача файлов
- административное управление пользователями

## Основные роли

- `GUEST` — авторизован, но ещё не подтверждён колледжем
- `STUDENT` — подтверждённый студент
- `TEACHER` — преподаватель
- `ADMIN` — администратор платформы

## Что уже есть

- JWT auth с access/refresh token
- глобальная обработка ошибок
- ролевая и ownership-авторизация
- enrollment для курсов
- course CRUD
- lesson CRUD
- markdown-поле `contentMd` у уроков
- файловые вложения для профиля, курса и урока
- Flyway миграции
- unit-тесты на критичные сервисы и security boundary
- OpenAPI / Swagger UI
- CI с прогоном тестов и валидацией миграций на чистой PostgreSQL

## Стек

- Java 25
- Spring Boot 4
- Spring Security
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway
- MapStruct
- JUnit 5 / Mockito
- Docker

## Документация

- Политика безопасности: [SECURITY.md](./SECURITY.md)
- OpenAPI JSON: `/v3/api-docs`
- OpenAPI YAML: `/v3/api-docs.yaml`
- Swagger UI: `/swagger-ui.html`

## Требования

Для локального запуска без Docker нужны:
- Java 25
- PostgreSQL

## Переменные окружения

Для standalone-запуска используется `.env.example` как шаблон.

Ключевые переменные:
- `PTKNOW_PORT`
- `PTKNOW_DATASOURCE_ADDRESS`
- `PTKNOW_DATASOURCE_NAME`
- `PTKNOW_DATASOURCE_USERNAME`
- `PTKNOW_DATASOURCE_PASSWORD`
- `JWT_ISSUER`
- `SECRET_JWT_KEY`
- `JWT_REFRESH_COOKIE_SECURE`
- `JWT_REFRESH_COOKIE_SAME_SITE`
- `APP_FILE_UPLOAD_DIR`

Spring читает именно environment variables.
Файл `.env` не считывается Spring Boot автоматически.
Значения из `.env` применяются только в тех сценариях, где этот файл предварительно загружается во внешние переменные окружения, например через Docker Compose или конфигурацию запуска IDE/оболочки.

## Локальный запуск

1. Поднять PostgreSQL.
2. Настроить env-переменные по `.env.example`.
3. Запустить приложение:

```bash
./gradlew bootRun
```

Или через запуск `PTKnowApplication` из IDE.

## Запуск в Docker

В этом репозитории остаётся только backend-образ:

```bash
docker build -t ptknow-server .
```

Полный deploy через `compose` вынесен в отдельный [deploy-репозиторий](https://github.com/ZapolyarnyDev/PTKnowDeployment).

## Тесты

Запуск тестов:

```bash
./gradlew test
```

## Миграции

Flyway применяется автоматически при старте приложения.
Текущий режим JPA:
- `ddl-auto=validate`

Это означает:
- схема должна приезжать из миграций
- Hibernate только валидирует соответствие сущностей схеме

## Замечания по разработке

- правила доступа должны проверяться не только в controller, но и в service/policy layer
- новые изменения в доменной модели должны сопровождаться миграциями Flyway
- security-правила нужно синхронизировать с `SECURITY.md`
- deploy orchestration хранится отдельно от backend-репозитория
