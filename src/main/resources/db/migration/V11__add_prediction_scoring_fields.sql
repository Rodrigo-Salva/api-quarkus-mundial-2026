-- Campos para el sistema de puntuación completo del lab
ALTER TABLE predictions
    ADD COLUMN IF NOT EXISTS settled         BOOLEAN   NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS early_bonus     INTEGER   NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS streak_bonus    INTEGER   NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_points    INTEGER   NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS winner_correct  BOOLEAN   NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_predictions_user_settled
    ON predictions(user_id, settled, created_at);
