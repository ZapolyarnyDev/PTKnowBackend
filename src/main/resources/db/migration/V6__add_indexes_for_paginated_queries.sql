CREATE INDEX IF NOT EXISTS idx_lesson_course_id_begin_at
    ON lesson (course_id, begin_at);

CREATE INDEX IF NOT EXISTS idx_file_attachments_resource_type_resource_id
    ON file_attachments (resource_type, resource_id);

CREATE INDEX IF NOT EXISTS idx_file_attachments_file_id
    ON file_attachments (file_id);

CREATE INDEX IF NOT EXISTS idx_course_editors_mapping_editor_id_course_id
    ON course_editors_mapping (editor_id, course_id);

CREATE INDEX IF NOT EXISTS idx_course_tags_mapping_tag_id_course_id
    ON course_tags_mapping (tag_id, course_id);

CREATE INDEX IF NOT EXISTS idx_courses_state
    ON courses (state);

CREATE INDEX IF NOT EXISTS idx_courses_owner_id
    ON courses (owner_id);

CREATE INDEX IF NOT EXISTS idx_auth_data_status_role_registered_at
    ON auth_data (status, role, registered_at DESC);
