# 🔒 Политика безопасности PTKnow

Документ фиксирует текущую модель доступа в PTKnow. Его задача — дать однозначный ответ, кто и к каким точкам API имеет доступ, чтобы это можно было сверять с `SecurityConfig`, контроллерами, сервисами и слоем политик.

## 👤 Роли доступа

- `ANONYMOUS` - пользователь не вошёл в систему
- `GUEST` - пользователь вошёл в систему, но ещё не подтверждён колледжем
- `STUDENT` - подтверждённый студент
- `TEACHER` - преподаватель
- `ADMIN` - администратор платформы

## 🧩 Владение ресурсом и права редактора

`OWNER` и `EDITOR` не являются глобальными ролями.

`OWNER` - контекстное право владения конкретным ресурсом:
- `OWNER(profile)` = `profile.userId == currentUser.id`
- `OWNER(course)` = `course.ownerId == currentUser.id`
- `OWNER(lesson)` = `lesson.ownerId == currentUser.id`
- `OWNER(file)` = файл принадлежит профилю, курсу или уроку, владельцем которого является текущий пользователь

Правила:
- владение ресурсом не должно проверяться только в контроллере
- владение ресурсом должно подтверждаться в сервисе или слое политик
- `ADMIN` имеет доступ ко всем ресурсам независимо от владения

`EDITOR` — контекстное право редактирования конкретного курса:
- `EDITOR(course)` = пользователь добавлен в список редакторов курса

Правила:
- `EDITOR` не является глобальной ролью
- права редактора должны проверяться в сервисе или слое политик
- `ADMIN` имеет доступ к изменению всех ресурсов независимо от статуса редактора

## 🔁 Переходы ролей

Целевая модель ролей:
- `ANONYMOUS -> GUEST` после регистрации
- `GUEST -> STUDENT` после подтверждения колледжем
- `GUEST -> TEACHER` после подтверждения колледжем или назначения администратором
- `STUDENT -> GUEST` и `TEACHER -> GUEST` при отзыве подтверждения
- `ADMIN` назначается только вручную

В текущей реализации уже есть:
- административное изменение роли
- журналирование изменения роли
- запрет на самоназначение привилегированной роли

## 🚫 Семантика ошибок доступа

- `401 Unauthorized` — пользователь не вошёл в систему или токен недействителен
- `403 Forbidden` — пользователь вошёл в систему, но не имеет права на действие
- `404 Not Found` — ресурс не существует

Принципы:
- ошибки доступа не должны превращаться в `500`
- внутренние ошибки не должны маскироваться под `401`

## 📝 Запись на курс

enrollment — запись пользователя на курс.

`ENROLLED` — пользователь записан на конкретный курс.

Правила:
- проверка записи на курс выполняется в сервисе
- запись на курс не заменяет владение, права редактора и права администратора, а дополняет их

## 🧱 Технические инварианты

- каждая точка API, изменяющая состояние, обязана иметь явное правило доступа
- каждая проверка владения ресурсом должна находиться в сервисе или слое политик
- `ADMIN` имеет полный доступ ко всем ресурсам
- access- и refresh-токены не должны логироваться целиком
- refresh-токены в базе хранятся только в виде хэша
- в рабочем окружении refresh-cookie должны быть `HttpOnly` и `Secure`
- правила в коде должны соответствовать этому документу

## 📌 Статусы

- `Сделано` — правило реализовано и соответствует текущему коду
- `Не сделано` — правило описано, но ещё не реализовано
- `Нет в доменной модели` — для реализации правила не хватает сущности, связи или части модели

## 🗺️ Матрица доступа

### Аутентификация

- `POST /api/v0/auth/register` — `ANONYMOUS` — `Сделано`
- `POST /api/v0/auth/login` — `ANONYMOUS` — `Сделано`
- `POST /api/v0/auth/logout` — любой пользователь, вошедший в систему: `GUEST`, `STUDENT`, `TEACHER`, `ADMIN` — `Сделано`
- `POST /api/v0/token/refresh` — `ANONYMOUS` или пользователь, вошедший в систему, при наличии действительного refresh-токена — `Сделано`

### Профиль

- `GET /api/v0/profile` — `GUEST`, `STUDENT`, `TEACHER`, `ADMIN` — `Сделано`
- `GET /api/v0/profile/me` — `OWNER(profile)` — `Сделано`
- `GET /api/v0/profile/search` — `ANONYMOUS`, `GUEST`, `STUDENT`, `TEACHER`, `ADMIN` — `Сделано`
- `GET /api/v0/profile/{handle}` — `ANONYMOUS`, `GUEST`, `STUDENT`, `TEACHER`, `ADMIN` — `Сделано`
- `GET /api/v0/profile/id/{userId}` — `ADMIN`, `OWNER(profile)` — `Сделано`
- `PATCH /api/v0/profile` — `OWNER(profile)` — `Сделано`
- `PUT /api/v0/profile` — `OWNER(profile)` — `Сделано`
- `POST /api/v0/profile/avatar` — `OWNER(profile)` — `Сделано`
- `DELETE /api/v0/profile/avatar` — `OWNER(profile)` — `Сделано`

### Файлы

- `GET /api/v0/files/{id}` — доступ зависит от `fileVisibility` вложения и видимости родительского ресурса — `Сделано`
- `PUBLIC` — `ANONYMOUS`, `GUEST`, `STUDENT`, `TEACHER`, `ADMIN` — `Сделано`
- `ENROLLED` — `ENROLLED`, `OWNER(course|lesson)`, `EDITOR(course)`, `ADMIN`; анонимный доступ возможен только для опубликованного родительского курса и только если это допускает общая политика чтения — `Сделано`
- `PRIVATE` — `OWNER(profile|course|lesson)`, `ADMIN` — `Сделано`
- `GET /api/v0/files/{id}/meta` — `OWNER(file)`, `ADMIN` — `Сделано`
- `DELETE /api/v0/files/{id}` — `OWNER(file)`, `ADMIN` — `Сделано`

### Курсы

- `GET /api/v0/course` — `ANONYMOUS`, `GUEST`, `STUDENT`, `TEACHER`, `ADMIN`; для анонимных и обычных пользователей доступны только опубликованные курсы, а владельцы и редакторы видят свои черновики и архивные курсы через правила сервиса — `Сделано`
- `POST /api/v0/course` — `TEACHER`, `ADMIN` — `Сделано`
- `PATCH /api/v0/course/{id}` — `OWNER(course)`, `ADMIN` — `Сделано`
- `PUT /api/v0/course/{id}` — `OWNER(course)`, `ADMIN` — `Сделано`
- `GET /api/v0/course/id/{id}` — `ANONYMOUS` для `PUBLISHED`, `ENROLLED`, `OWNER(course)`, `EDITOR(course)`, `ADMIN`; `DRAFT` и `ARCHIVED` недоступны пользователям без управляющих прав — `Сделано`
- `GET /api/v0/course/handle/{handle}` — те же правила, что и для `GET /api/v0/course/id/{id}` — `Сделано`
- `POST /api/v0/course/{id}/preview` — `OWNER(course)`, `EDITOR(course)`, `ADMIN`; видимость preview синхронизируется с состоянием курса — `Сделано`
- `DELETE /api/v0/course/{id}` — `OWNER(course)`, `ADMIN` — `Сделано`
- `POST /api/v0/course/{id}/editors/{userId}` — `OWNER(course)`, `ADMIN` — `Сделано`
- `DELETE /api/v0/course/{id}/editors/{userId}` — `OWNER(course)`, `ADMIN` — `Сделано`
- `POST /api/v0/course/{id}/publish` — `OWNER(course)`, `ADMIN` — `Сделано`
- `POST /api/v0/course/{id}/archive` — `OWNER(course)`, `ADMIN` — `Сделано`
- `POST /api/v0/course/{id}/enroll` — `GUEST`, `STUDENT`; дополнительно применяются ограничения записи на курс — `Сделано`
- `DELETE /api/v0/course/{id}/enroll` — пользователь может отменить только собственную запись на курс — `Сделано`
- `GET /api/v0/course/{id}/members` — `OWNER(course)`, `EDITOR(course)`, `ENROLLED`, `ADMIN` — `Сделано`
- `GET /api/v0/course/{id}/students` — `OWNER(course)`, `ADMIN` — `Сделано`
- `GET /api/v0/course/{id}/teachers` — `OWNER(course)`, `ADMIN` — `Сделано`
- `POST /api/v0/course/{id}/teachers` — `OWNER(course)`, `ADMIN` — `Сделано`
- `DELETE /api/v0/course/{id}/teachers/{teacherId}` — `OWNER(course)`, `ADMIN` — `Сделано`

### Уроки

`contentMd` является частью ресурса `lesson` и подчиняется тем же правилам доступа, что и сам урок.

- `POST /api/v0/lessons/{courseId}` — `OWNER(course)`, `EDITOR(course)`, `ADMIN` — `Сделано`
- `GET /api/v0/lessons/{lessonId}` — `ANONYMOUS` для уроков опубликованного курса, `ENROLLED`, `OWNER(course)`, `EDITOR(course)`, `ADMIN` — `Сделано`
- `GET /api/v0/lessons/course/{courseId}` — `ANONYMOUS` для опубликованного курса, `ENROLLED`, `OWNER(course)`, `EDITOR(course)`, `ADMIN` — `Сделано`
- `PATCH /api/v0/lessons/{lessonId}` — `OWNER(lesson)`, `ADMIN` — `Сделано`
- `PUT /api/v0/lessons/{lessonId}` — `OWNER(lesson)`, `ADMIN` — `Сделано`
- `PATCH /api/v0/lessons/{lessonId}/state` — `OWNER(lesson)`, `ADMIN` — `Сделано`
- `DELETE /api/v0/lessons/{lessonId}` — `OWNER(lesson)`, `OWNER(course)`, `ADMIN` — `Сделано`
- `POST /api/v0/lessons/{lessonId}/materials` — `OWNER(lesson)`, `ADMIN` — `Сделано`
- `DELETE /api/v0/lessons/{lessonId}/materials/{fileId}` — `OWNER(lesson)`, `ADMIN` — `Сделано`

### Администрирование пользователей

- `GET /api/v0/users` — `ADMIN` — `Сделано`
- `GET /api/v0/users/{id}` — `ADMIN` — `Сделано`
- `PATCH /api/v0/users/{id}/role` — `ADMIN`; самоназначение привилегированной роли запрещено — `Сделано`
- `PATCH /api/v0/users/{id}/status` — `ADMIN`; запрет на блокировку самого себя реализован — `Сделано`
