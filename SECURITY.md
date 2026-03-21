# Политика безопасности PTKnow

Документ фиксирует текущую и целевую модель авторизации для PTKnow
Цель документа: однозначно определить, кто и к каким endpoint-ам имеет доступ, чтобы это можно было напрямую реализовать в policy/service-layer проверках

## Субъекты доступа

- `ANONYMOUS` - пользователь не аутентифицирован
- `GUEST` - пользователь аутентифицирован, но не подтвержден колледжем
- `STUDENT` - подтвержденный студент колледжа
- `TEACHER` - преподаватель колледжа
- `ADMIN` - администратор платформы

## Роли и право владения ресурсом

`OWNER` и `EDITOR` не являются глобальными ролями.

`OWNER` - это контекстное право владения конкретным ресурсом, которое может быть только у автора ресурса:

- `OWNER(profile)` = `profile.userId == currentUser.id`
- `OWNER(course)` = `course.ownerId == currentUser.id`
- `OWNER(lesson)` = `lesson.ownerId == currentUser.id`
- `OWNER(file)` = файл принадлежит профилю, курсу или уроку, владельцем которого является текущий пользователь

Следствия:

- пользователь не может иметь глобальную роль `OWNER`
- ownership должен проверяться в policy/service layer, а не только на уровне controller
- `ADMIN` имеет доступ ко всем ресурсам независимо от ownership

`EDITOR` - это опциональное контекстное право на изменение конкретного ресурса, которое может быть у нескольких пользователей:

- `EDITOR(course)` = `course.hasEditor(currentUser.id)`

Следствия:

- пользователь не может иметь глобальную роль `EDITOR`
- право на изменение ресурса должно проверяться в policy/service layer, а не только на уровне controller
- `ADMIN` имеет доступ к изменению всех ресурсов

## Переходы ролей

На текущем этапе целевая модель переходов ролей такая:

- `ANONYMOUS -> GUEST` после регистрации
- `GUEST -> STUDENT` через подтверждение колледжем
- `GUEST -> TEACHER` через подтверждение колледжем или назначение администратором
- `STUDENT -> GUEST` и `TEACHER -> GUEST` при отзыве подтверждения
- `ADMIN` назначается только вручную и вне публичного self-service процесса

Реализовано в текущей версии:

- endpoint или admin-flow изменения роли
- аудит изменения ролей
- запрет на самоназначение привилегированной роли

## Семантика ошибок авторизации

- `401 Unauthorized` - пользователь не аутентифицирован или access token невалиден
- `403 Forbidden` - пользователь аутентифицирован, но не имеет прав на действие
- `404 Not Found` - ресурс не существует

Принцип:

- ошибки доступа не должны маскироваться под `500`
- внутренние ошибки не должны маскироваться под `401`

## Enrollment

enrollment - зачисление/запись пользователя на курсы, мероприятия, проекты, в контексте приложения - отдельная сущность или связь

`ENROLLED` - пользователь принадлежащий образовательному ресурсу

enroll - запись субъекта на ресурс
unenroll - отмена записи субъекта на ресурс

Доступ к enroll/unenroll должен быть описан явно для каждого ресурса

## Технические инварианты

- каждый mutating endpoint обязан иметь явное правило доступа
- каждая проверка на владение ресурсом должна быть реализована в service/policy layer
- любой `ADMIN` имеет полный доступ ко всем ресурсам
- refresh/access токены не логируются целиком
- cookie refresh token в production должны быть `HttpOnly` и `Secure`
- security-правила в коде должны соответствовать этому документу

## Статусы реализации

- `Сделано` - endpoint и правило доступа уже поддерживаются текущим кодом
- `Не сделано` - endpoint существует, но правило доступа из документа еще не реализовано
- `Нет в доменной модели` - для реализации правила не хватает сущностей, связей или атрибутов доменной модели

## Матрица доступа: текущие endpoint-ы

Ниже перечислены все endpoint-ы, которые уже существуют в контроллерах проекта.

### AuthController

- `POST /v0/auth/register` - `ANONYMOUS` - `Сделано`
- `POST /v0/auth/login` - `ANONYMOUS` - `Сделано`
- `POST /v0/auth/logout` - любой аутентифицированный пользователь: `GUEST`, `STUDENT`, `TEACHER`, `ADMIN` - `Сделано`

### TokenController

- `POST /v0/token/refresh` - `ANONYMOUS` или аутентифицированный пользователь с валидным refresh token - `Сделано`

### ProfileController

- `GET /v0/profile` - `GUEST`, `STUDENT`, `TEACHER`, `ADMIN` - `Сделано`
- `GET /v0/profile/me` - `OWNER(profile)` - `Сделано`
- `GET /v0/profile/{handle}` - `GUEST`, `STUDENT`, `TEACHER`, `ADMIN` - `Сделано`
- `GET /v0/profile/id/{userId}` - `ADMIN`, `OWNER(profile)` - `Сделано`
- `PATCH /v0/profile` - `OWNER(profile)` - `Сделано`
- `PUT /v0/profile` - `OWNER(profile)` - `Сделано`
- `POST /v0/profile/avatar` - `OWNER(profile)` - `Сделано`
- `DELETE /v0/profile/avatar` - `OWNER(profile)` - `Сделано`

### FileController

- `GET /v0/files/{id}` - зависит от `fileVisibility` attachment-а - `Сделано`
- `PUBLIC` (например, аватар профиля) - `GUEST`, `STUDENT`, `TEACHER`, `ADMIN` - `Сделано`
- `ENROLLED` (например, preview курса) - `ENROLLED`, `OWNER(course|lesson)`, `EDITOR(course)`, `ADMIN`; дополнительно зависит от `state` ресурса (`PUBLISHED`) - `Сделано`
- `PRIVATE` - `OWNER(profile|course|lesson)`, `ADMIN` - `Сделано`
- `GET /v0/files/{id}/meta` - `OWNER(file)`, `ADMIN` - `Сделано`
- `DELETE /v0/files/{id}` - `OWNER(file)`, `ADMIN` - `Сделано`

### CourseController

- `GET /v0/course` - `GUEST`, `STUDENT`, `TEACHER`, `ADMIN`; в выдаче учитывается `state` (`PUBLISHED` для каталога, + персонально доступные курсы) - `Сделано`
- `POST /v0/course` - `TEACHER`, `ADMIN` - `Сделано`
- `PATCH /v0/course/{id}` - `OWNER(course)`, `ADMIN` - `Сделано`
- `PUT /v0/course/{id}` - `OWNER(course)`, `ADMIN` - `Сделано`
- `GET /v0/course/id/{id}` - `ENROLLED`, `OWNER(course)`, `EDITOR(course)`, `ADMIN`; дополнительно зависит от `state` (`DRAFT/ARCHIVED` недоступны не-управляющим) - `Сделано`
- `GET /v0/course/handle/{handle}` - аналогично `GET /v0/course/id/{id}` - `Сделано`
- `POST /v0/course/{id}/preview` - `OWNER(course)`, `EDITOR(course)` , `ADMIN`; visibility preview синхронизируется с `state` - `Сделано`
- `DELETE /v0/course/{id}` - `OWNER(course)`, `ADMIN` - `Сделано`
- `POST /v0/course/{id}/editors/{userId}` - `OWNER(course)`, `ADMIN` - `Сделано`
- `DELETE /v0/course/{id}/editors/{userId}` - `OWNER(course)`, `ADMIN` - `Сделано`
- `POST /v0/course/{id}/publish` - `OWNER(course)`, `ADMIN` - `Сделано`
- `POST /v0/course/{id}/archive` - `OWNER(course)`, `ADMIN` - `Сделано`
- `POST /v0/course/{id}/enroll` - `GUEST`, `STUDENT`; дополнительно применяются business-ограничения enrollment (не повторно, не при переполнении курса) - `Сделано`
- `DELETE /v0/course/{id}/enroll` - сам записанный пользователь (`GUEST`, `STUDENT`) - `Сделано`
- `GET /v0/course/{id}/members` - `OWNER(course)`, `EDITOR(course)`, `ENROLLED`, `ADMIN` - `Сделано`
- `GET /v0/course/{id}/students` - `OWNER(course)`, `ADMIN` - `Сделано`
- `GET /v0/course/{id}/teachers` - `OWNER(course)`, `ADMIN` - `Сделано`
- `POST /v0/course/{id}/teachers` - `OWNER(course)`, `ADMIN` - `Сделано`
- `DELETE /v0/course/{id}/teachers/{teacherId}` - `OWNER(course)`, `ADMIN` - `Сделано`

### LessonController

`contentMd` является частью ресурса `lesson` и подчиняется тем же правилам доступа, что и чтение/изменение урока: виден через `GET lesson` и `GET lessons by course`, изменяется только через `PATCH/PUT lesson`.

- `POST /v0/lessons/{courseId}` - `OWNER(course)`, `EDITOR(course)`, `ADMIN` - `Сделано`
- `GET /v0/lessons/{lessonId}` - `OWNER(course)`, `EDITOR(course)`, `ENROLLED`, `ADMIN` - `Сделано`
- `GET /v0/lessons/course/{courseId}` - `OWNER(course)`, `EDITOR(course)`, `ENROLLED`, `ADMIN` - `Сделано`
- `PATCH /v0/lessons/{lessonId}` - `OWNER(lesson)`, `ADMIN` - `Сделано`
- `PUT /v0/lessons/{lessonId}` - `OWNER(lesson)`, `ADMIN` - `Сделано`
- `PATCH /v0/lessons/{lessonId}/state` - `OWNER(lesson)`, `ADMIN` - `Сделано`
- `DELETE /v0/lessons/{lessonId}` - `OWNER(lesson)`, `OWNER(course)`, `ADMIN` - `Сделано`
- `POST /v0/lessons/{lessonId}/materials` - `OWNER(lesson)`, `ADMIN` - `Сделано`
- `DELETE /v0/lessons/{lessonId}/materials/{fileId}` - `OWNER(lesson)`, `ADMIN` - `Сделано`

### UserAdminController

- `GET /v0/users` - `ADMIN` - `Сделано`
- `GET /v0/users/{id}` - `ADMIN` - `Сделано`
- `PATCH /v0/users/{id}/role` - `ADMIN`; самоназначение роли запрещено - `Сделано`
- `PATCH /v0/users/{id}/status` - `ADMIN`; self-block запрещен - `Сделано`
