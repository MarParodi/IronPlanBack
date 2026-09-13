-- Convierte timestamps de actividad/fuerza de UTC naive a hora de Los Mochis (UTC-7).
-- Idempotente en runtime: ShiftActivityTimestampsToMazatlan solo lo aplica una vez.
-- No ejecutar a mano si el parche Java ya corrió (restaría otras 7 horas).

CREATE TABLE IF NOT EXISTS ironplan_data_patches (
    patch_name VARCHAR(120) NOT NULL PRIMARY KEY,
    applied_at DATETIME(6) NOT NULL
);

UPDATE free_activity_sessions
SET started_at = TIMESTAMPADD(HOUR, -7, started_at),
    completed_at = TIMESTAMPADD(HOUR, -7, completed_at),
    created_at = TIMESTAMPADD(HOUR, -7, created_at);

UPDATE workout_sessions
SET started_at = TIMESTAMPADD(HOUR, -7, started_at),
    completed_at = TIMESTAMPADD(HOUR, -7, completed_at),
    created_at = TIMESTAMPADD(HOUR, -7, created_at),
    updated_at = IF(updated_at IS NULL, NULL, TIMESTAMPADD(HOUR, -7, updated_at));

UPDATE workout_exercises
SET started_at = IF(started_at IS NULL, NULL, TIMESTAMPADD(HOUR, -7, started_at)),
    finished_at = IF(finished_at IS NULL, NULL, TIMESTAMPADD(HOUR, -7, finished_at));

UPDATE user_activities ua
INNER JOIN free_activity_sessions f ON f.id = ua.source_id AND f.user_id = ua.user_id
SET ua.activity_date = DATE(f.completed_at)
WHERE ua.metric_type IN ('FREE_ACTIVITY_COUNT', 'FREE_ACTIVITY_KM');

UPDATE user_activities ua
INNER JOIN workout_sessions w ON w.id = ua.source_id AND w.user_id = ua.user_id
SET ua.activity_date = DATE(COALESCE(w.completed_at, w.started_at))
WHERE ua.metric_type = 'VOLUME_TOTAL';

UPDATE user_activities ua
INNER JOIN free_activity_sessions f ON f.id = ua.source_id AND f.user_id = ua.user_id
SET ua.activity_date = DATE(f.completed_at)
WHERE ua.metric_type IN ('SESSIONS', 'WORKOUTS_COUNT', 'ACTIVE_MINUTES')
  AND NOT EXISTS (
      SELECT 1 FROM workout_sessions w
      WHERE w.id = ua.source_id AND w.user_id = ua.user_id
  );

UPDATE user_activities ua
INNER JOIN workout_sessions w ON w.id = ua.source_id AND w.user_id = ua.user_id
SET ua.activity_date = DATE(COALESCE(w.completed_at, w.started_at))
WHERE ua.metric_type IN ('SESSIONS', 'WORKOUTS_COUNT', 'ACTIVE_MINUTES')
  AND NOT EXISTS (
      SELECT 1 FROM free_activity_sessions f
      WHERE f.id = ua.source_id AND f.user_id = ua.user_id
  );

INSERT INTO ironplan_data_patches (patch_name, applied_at)
VALUES ('utc_to_mazatlan_minus_7h_v1', UTC_TIMESTAMP(6));
