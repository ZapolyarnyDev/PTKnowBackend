# Политика безопасности PTKnow

Документ фиксирует текущую модель доступа и целевые правила авторизации для PTKnow.
Цель документа: однозначно определить, кто и к каким endpoint-ам имеет доступ, чтобы это можно было сверять с реализацией в `SecurityConfig`, controller-layer и service/policy-layer.

## Общие роли доступа

- `ANONYMOUS` - пользователь не аутентифицирован
- `GUEST` - пользователь аутентифицирован, но ещё не подтверждён колледжем
- `STUDENT` - подтверждённый студент
- `TEACHER` - преподаватель
- `ADMIN` - администратор платформы

## Ownership и editor-права

`OWNER` и `EDITOR` не являются глобальными ролями.

`OWNER` - это контекстное право владения конкретным ресурсом:
- `OWNER(profile)` = `profile.userId == currentUser.id`
- `OWNER(course)` = `course.ownerId == currentUser.id`
- `OWNER(lesson)` = `lesson.ownerId == currentUser.id`
- `OWNER(file)` = файл принадлежит профилю, курсу или уроку, владельцем которого является текущий пользователь

Правила:
- ownership не должен проверяться только на controller-layer
- ownership должен подтверждаться в service/policy-layer
- `ADMIN` имеет доступ ко всем ресурсам независимо от ownership

`EDITOR` - это контекстное право редактирования конкретного курса:
- `EDITOR(course)` = пользователь добавлен в editors курса

Правила:
- `EDITOR` не является глобальной ролью
- editor-права должны проверяться в service/policy-layer
- `ADMIN` имеет доступ к изменению всех ресурсов независимо от editor-статуса

## Переходы ролей

На текущем этапе целевая модель ролей такая:
- `ANONYMOUS -> GUEST` после регистрации
- `GUEST -> STUDENT` через подтверждение колледжем
- `GUEST -> TEACHER` через подтверждение колледжем или назначение администратором
- `STUDENT -> GUEST` и `TEACHER -> GUEST` при отзыве подтверждения
- `ADMIN` назначается только вручную и не получает публичного self-service процесса

Реализовано в текущей версии:
- admin-flow изменения роли
- audit изменения роли
- запрет на самоназначение привилегированной роли

## Семантика ошибок авторизации

- `401 Unauthorized` - пользователь не аутентифицирован или access token невалиден
- `403 Forbidden` - пользователь аутентифицирован, но не имеет права на действие
- `404 Not Found` - ресурс не существует

Принципы:
- ошибки доступа не должны маскироваться под `500`
- внутренние ошибки не должны маскироваться под `401`

## Enrollment

enrollment - запись пользователя на курс.

`ENROLLED` - пользователь записан на конкретный курс.

- `enroll` - создать запись на курс
- `unenroll` - удалить свою запись с курса

Правила:
- enrollment проверяется в service-layer
- enrollment не заменяет ownership/editor/admin-права, а дополняет их

## Технические инварианты

- каждый mutating endpoint обязан иметь явное правило доступа
- каждая проверка владения ресурсом должна быть реализована в service/policy-layer
- `ADMIN` имеет полный доступ ко всем ресурсам
- refresh/access токены не логируются целиком
- refresh token в БД хранится в виде хэша
- production refresh cookie должны быть `HttpOnly` и `Secure`
- правила в коде должны соответствовать этому документу

## Статусы

- `Сделано` - правило реализовано и соответствует текущему коду
- `Не сделано` - правило описано, но ещё не реализовано
- `Нет в доменной модели` - для реализации правила не хватает сущности, связи или доменной модели

## Матрица доступа: текущие endpoint-ы

### AuthController

- `POST /api/v0/auth/register` - `ANONYMOUS` - `Сделано`
- `POST /api/v0/auth/login` - `ANONYMOUS` - `Сделано`
- `POST /api/v0/auth/logout` - любой аутентифицированный пользователь: `GUEST`, `STUDENT`, `TEACHER`, `ADMIN` - `Сделано`

### TokenController

- `POST /api/v0/token/refresh` - `ANONYMOUS` или аутентифицированный пользователь с валидным refresh token - `Сделано`

### ProfileController

- `GET /api/v0/profile` - `GUEST`, `STUDENT`, `TEACHER`, `ADMIN` - `Сделано`
- `GET /api/v0/profile/me` - `OWNER(profile)` - `Сделано`
- `GET /api/v0/profile/search` - `ANONYMOUS`, `GUEST`, `STUDENT`, `TEACHER`, `ADMIN` - `Сделано`
- `GET /api/v0/profile/{handle}` - `ANONYMOUS`, `GUEST`, `STUDENT`, `TEACHER`, `ADMIN` - `Сделано`
- `GET /api/v0/profile/id/{userId}` - `ADMIN`, `OWNER(profile)` - `Сделано`
- `PATCH /api/v0/profile` - `OWNER(profile)` - `Сделано`
- `PUT /api/v0/profile` - `OWNER(profile)` - `Сделано`
- `POST /api/v0/profile/avatar` - `OWNER(profile)` - `Сделано`
- `DELETE /api/v0/profile/avatar` - `OWNER(profile)` - `Сделано`

### FileController

- `GET /api/v0/files/{id}` - доступ зависит от `fileVisibility` attachment-а и видимости родительского ресурса - `Сделано`
- `PUBLIC` - `ANONYMOUS`, `GUEST`, `STUDENT`, `TEACHER`, `ADMIN` - `Сделано`
- `ENROLLED` - `ENROLLED`, `OWNER(course|lesson)`, `EDITOR(course)`, `ADMIN`; анонимный доступ возможен только если родительский курс опубликован и ресурс доступен по общей read-policy - `Сделано`
- `PRIVATE` - `OWNER(profile|course|lesson)`, `ADMIN` - `Сделано`
- `GET /api/v0/files/{id}/meta` - `OWNER(file)`, `ADMIN` - `Сделано`
- `DELETE /api/v0/files/{id}` - `OWNER(file)`, `ADMIN` - `Сделано`

### CourseController

- `GET /api/v0/course` - `ANONYMOUS`, `GUEST`, `STUDENT`, `TEACHER`, `ADMIN`; для анонимных и обычных пользователей доступны только опубликованные курсы, manager-ы видят свои `DRAFT/ARCHIVED` через service-layer policy - `Сделано`
- `POST /api/v0/course` - `TEACHER`, `ADMIN` - `Сделано`
- `PATCH /api/v0/course/{id}` - `OWNER(course)`, `ADMIN` - `Сделано`
- `PUT /api/v0/course/{id}` - `OWNER(course)`, `ADMIN` - `Сделано`
- `GET /api/v0/course/id/{id}` - `ANONYMOUS` для `PUBLISHED`, `ENROLLED`, `OWNER(course)`, `EDITOR(course)`, `ADMIN`; `DRAFT/ARCHIVED` недоступны не-manager пользователям - `Сделано`
- `GET /api/v0/course/handle/{handle}` - те же правила, что и для `GET /api/v0/course/id/{id}` - `Сделано`
- `POST /api/v0/course/{id}/preview` - `OWNER(course)`, `EDITOR(course)`, `ADMIN`; visibility preview синхронизируется с `state` - `Сделано`
- `DELETE /api/v0/course/{id}` - `OWNER(course)`, `ADMIN` - `Сделано`
- `POST /api/v0/course/{id}/editors/{userId}` - `OWNER(course)`, `ADMIN` - `Сделано`
- `DELETE /api/v0/course/{id}/editors/{userId}` - `OWNER(course)`, `ADMIN` - `Сделано`
- `POST /api/v0/course/{id}/publish` - `OWNER(course)`, `ADMIN` - `Сделано`
- `POST /api/v0/course/{id}/archive` - `OWNER(course)`, `ADMIN` - `Сделано`
- `POST /api/v0/course/{id}/enroll` - `GUEST`, `STUDENT`; дополнительно применяются business-ограничения enrollment - `Сделано`
- `DELETE /api/v0/course/{id}/enroll` - свой записанный пользователь (`GUEST`, `STUDENT`) - `Сделано`
- `GET /api/v0/course/{id}/members` - `OWNER(course)`, `EDITOR(course)`, `ENROLLED`, `ADMIN` - `Сделано`
- `GET /api/v0/course/{id}/students` - `OWNER(course)`, `ADMIN` - `Сделано`
- `GET /api/v0/course/{id}/teachers` - `OWNER(course)`, `ADMIN` - `Сделано`
- `POST /api/v0/course/{id}/teachers` - `OWNER(course)`, `ADMIN` - `Сделано`
- `DELETE /api/v0/course/{id}/teachers/{teacherId}` - `OWNER(course)`, `ADMIN` - `Сделано`

### LessonController

`contentMd` является частью ресурса `lesson` и подчиняется тем же правилам доступа, что и чтение или изменение урока.

- `POST /api/v0/lessons/{courseId}` - `OWNER(course)`, `EDITOR(course)`, `ADMIN` - `Сделано`
- `GET /api/v0/lessons/{lessonId}` - `ANONYMOUS` для уроков опубликованного курса, `ENROLLED`, `OWNER(course)`, `EDITOR(course)`, `ADMIN` - `Сделано`
- `GET /api/v0/lessons/course/{courseId}` - `ANONYMOUS` для опубликованного курса, `ENROLLED`, `OWNER(course)`, `EDITOR(course)`, `ADMIN` - `Сделано`
- `PATCH /api/v0/lessons/{lessonId}` - `OWNER(lesson)`, `ADMIN` - `Сделано`
- `PUT /api/v0/lessons/{lessonId}` - `OWNER(lesson)`, `ADMIN` - `Сделано`
- `PATCH /api/v0/lessons/{lessonId}/state` - `OWNER(lesson)`, `ADMIN` - `Сделано`
- `DELETE /api/v0/lessons/{lessonId}` - `OWNER(lesson)`, `OWNER(course)`, `ADMIN` - `Сделано`
- `POST /api/v0/lessons/{lessonId}/materials` - `OWNER(lesson)`, `ADMIN` - `Сделано`
- `DELETE /api/v0/lessons/{lessonId}/materials/{fileId}` - `OWNER(lesson)`, `ADMIN` - `Сделано`

### UserAdminController

- `GET /api/v0/users` - `ADMIN` - `Сделано`
- `GET /api/v0/users/{id}` - `ADMIN` - `Сделано`
- `PATCH /api/v0/users/{id}/role` - `ADMIN`; самоназначение привилегированной роли запрещено - `Сделано`
- `PATCH /api/v0/users/{id}/status` - `ADMIN`; self-block запрещён - `Сделано`
