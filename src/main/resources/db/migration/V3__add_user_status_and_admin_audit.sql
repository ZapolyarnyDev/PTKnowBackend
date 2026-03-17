ALTER TABLE auth_data
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';

CREATE TABLE IF NOT EXISTS user_admin_audit (
    id BIGSERIAL PRIMARY KEY,
    actor_id UUID NOT NULL,
    target_id UUID NOT NULL,
    action VARCHAR(64) NOT NULL,
    old_value VARCHAR(64) NOT NULL,
    new_value VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_user_admin_audit_actor FOREIGN KEY (actor_id) REFERENCES auth_data (id),
    CONSTRAINT fk_user_admin_audit_target FOREIGN KEY (target_id) REFERENCES auth_data (id)
);

CREATE INDEX IF NOT EXISTS idx_user_admin_audit_target_id ON user_admin_audit (target_id);
CREATE INDEX IF NOT EXISTS idx_user_admin_audit_created_at ON user_admin_audit (created_at);

