-- ═══════════════════════════════════════════════════════════════
-- SEED DATA — Mundial 2026 Predictions API
-- Solo se ejecuta en perfil %dev y %test (Flyway clean-at-start)
-- Contraseña de todos los usuarios: Test1234!
-- Hash SHA-256 de "Test1234!": 0fadf52a4580cfebb99e61162139af3d3a6403c1d36b83e4962b721d1c8cbd0b
-- ═══════════════════════════════════════════════════════════════

-- ── 1. USUARIOS ──────────────────────────────────────────────────
-- password para todos: Test1234!
INSERT INTO users (email, username, password_hash, country, role) VALUES
  ('admin@mundial2026.com',   'admin',      '0fadf52a4580cfebb99e61162139af3d3a6403c1d36b83e4962b721d1c8cbd0b', 'PER', 'ADMIN'),
  ('rodrigo@test.com',        'rodrigo26',  '0fadf52a4580cfebb99e61162139af3d3a6403c1d36b83e4962b721d1c8cbd0b', 'PER', 'USER'),
  ('carlos@test.com',         'carlitos10', '0fadf52a4580cfebb99e61162139af3d3a6403c1d36b83e4962b721d1c8cbd0b', 'ARG', 'USER'),
  ('maria@test.com',          'maria_gol',  '0fadf52a4580cfebb99e61162139af3d3a6403c1d36b83e4962b721d1c8cbd0b', 'BRA', 'USER'),
  ('juan@test.com',           'juanito7',   '0fadf52a4580cfebb99e61162139af3d3a6403c1d36b83e4962b721d1c8cbd0b', 'ESP', 'USER'),
  ('sofia@test.com',          'sofi_fit',   '0fadf52a4580cfebb99e61162139af3d3a6403c1d36b83e4962b721d1c8cbd0b', 'MEX', 'USER');

-- ── 2. PARTIDOS — Fase de Grupos Mundial 2026 ────────────────────
-- Grupo A
INSERT INTO matches (home_team, away_team, status, match_date) VALUES
  ('Argentina',  'Canadá',      'FINISHED', '2026-06-11 18:00:00'),
  ('México',     'Ecuador',     'FINISHED', '2026-06-11 21:00:00'),
  ('Argentina',  'Ecuador',     'FINISHED', '2026-06-15 18:00:00'),
  ('Canadá',     'México',      'FINISHED', '2026-06-15 21:00:00'),
  ('Ecuador',    'Canadá',      'FINISHED', '2026-06-19 18:00:00'),
  ('Argentina',  'México',      'FINISHED', '2026-06-19 18:00:00');

-- Grupo B
INSERT INTO matches (home_team, away_team, status, match_date) VALUES
  ('España',     'Brasil',      'FINISHED', '2026-06-12 15:00:00'),
  ('Japón',      'Croacia',     'FINISHED', '2026-06-12 18:00:00'),
  ('España',     'Croacia',     'FINISHED', '2026-06-16 15:00:00'),
  ('Brasil',     'Japón',       'FINISHED', '2026-06-16 18:00:00'),
  ('Croacia',    'Brasil',      'FINISHED', '2026-06-20 18:00:00'),
  ('España',     'Japón',       'FINISHED', '2026-06-20 18:00:00');

-- Grupo C (partidos programados — para que los usuarios puedan predecir)
INSERT INTO matches (home_team, away_team, status, match_date) VALUES
  ('Francia',    'Alemania',    'SCHEDULED', NOW() + INTERVAL '2 days'),
  ('Portugal',   'Marruecos',   'SCHEDULED', NOW() + INTERVAL '2 days' + INTERVAL '3 hours'),
  ('Francia',    'Marruecos',   'SCHEDULED', NOW() + INTERVAL '6 days'),
  ('Alemania',   'Portugal',    'SCHEDULED', NOW() + INTERVAL '6 days' + INTERVAL '3 hours'),
  ('Marruecos',  'Alemania',    'SCHEDULED', NOW() + INTERVAL '10 days'),
  ('Francia',    'Portugal',    'SCHEDULED', NOW() + INTERVAL '10 days');

-- Partido EN VIVO (para probar tiempo real)
INSERT INTO matches (home_team, away_team, status, home_score, away_score, match_date) VALUES
  ('Uruguay',    'Senegal',     'LIVE', 1, 0, NOW() - INTERVAL '1 hour');

-- ── 3. RESULTADOS de partidos FINISHED ───────────────────────────
UPDATE matches SET home_score = 3, away_score = 0 WHERE home_team = 'Argentina'  AND away_team = 'Canadá';
UPDATE matches SET home_score = 1, away_score = 1 WHERE home_team = 'México'     AND away_team = 'Ecuador';
UPDATE matches SET home_score = 2, away_score = 0 WHERE home_team = 'Argentina'  AND away_team = 'Ecuador';
UPDATE matches SET home_score = 2, away_score = 1 WHERE home_team = 'Canadá'     AND away_team = 'México';
UPDATE matches SET home_score = 1, away_score = 2 WHERE home_team = 'Ecuador'    AND away_team = 'Canadá';
UPDATE matches SET home_score = 1, away_score = 0 WHERE home_team = 'Argentina'  AND away_team = 'México';
UPDATE matches SET home_score = 2, away_score = 1 WHERE home_team = 'España'     AND away_team = 'Brasil';
UPDATE matches SET home_score = 0, away_score = 0 WHERE home_team = 'Japón'      AND away_team = 'Croacia';
UPDATE matches SET home_score = 3, away_score = 1 WHERE home_team = 'España'     AND away_team = 'Croacia';
UPDATE matches SET home_score = 2, away_score = 2 WHERE home_team = 'Brasil'     AND away_team = 'Japón';
UPDATE matches SET home_score = 1, away_score = 0 WHERE home_team = 'Croacia'    AND away_team = 'Brasil';
UPDATE matches SET home_score = 2, away_score = 0 WHERE home_team = 'España'     AND away_team = 'Japón';

-- ── 4. CUOTAS para partidos SCHEDULED ────────────────────────────
-- Francia vs Alemania
INSERT INTO odds_lines (match_id, market, odds, base_odds, total_staked, bet_count)
SELECT id, market, odds, odds, 0, 0 FROM matches,
  (VALUES
    ('HOME_WIN', 2.10), ('DRAW', 3.20), ('AWAY_WIN', 3.50),
    ('OVER_25', 1.80), ('UNDER_25', 2.00), ('BOTH_SCORE_YES', 1.90),
    ('BOTH_SCORE_NO', 1.85), ('CORNERS_OVER_95', 1.90), ('CORNERS_UNDER_95', 1.90),
    ('CARDS_OVER_35', 1.85), ('CARDS_UNDER_35', 1.90),
    ('FIRST_HALF_HOME', 3.00), ('FIRST_HALF_DRAW', 2.10), ('FIRST_HALF_AWAY', 4.50)
  ) AS t(market, odds)
WHERE home_team = 'Francia' AND away_team = 'Alemania';

-- Portugal vs Marruecos
INSERT INTO odds_lines (match_id, market, odds, base_odds, total_staked, bet_count)
SELECT id, market, odds, odds, 0, 0 FROM matches,
  (VALUES
    ('HOME_WIN', 1.75), ('DRAW', 3.50), ('AWAY_WIN', 4.20),
    ('OVER_25', 1.85), ('UNDER_25', 1.95), ('BOTH_SCORE_YES', 2.00),
    ('BOTH_SCORE_NO', 1.75), ('CORNERS_OVER_95', 1.90), ('CORNERS_UNDER_95', 1.90),
    ('CARDS_OVER_35', 1.85), ('CARDS_UNDER_35', 1.90),
    ('FIRST_HALF_HOME', 2.80), ('FIRST_HALF_DRAW', 2.20), ('FIRST_HALF_AWAY', 5.00)
  ) AS t(market, odds)
WHERE home_team = 'Portugal' AND away_team = 'Marruecos';

-- ── 5. PREDICCIONES de usuarios (partidos ya terminados) ─────────
-- rodrigo predice los 6 partidos de Argentina (acertó varios)
INSERT INTO predictions (user_id, match_id, home_score, away_score, points, early_bonus, streak_bonus, total_points, winner_correct, settled, created_at)
SELECT u.id, m.id, 3, 0, 5, 1, 0, 6, true, true, m.match_date - INTERVAL '2 days'
FROM users u, matches m WHERE u.username = 'rodrigo26' AND m.home_team = 'Argentina' AND m.away_team = 'Canadá';

INSERT INTO predictions (user_id, match_id, home_score, away_score, points, early_bonus, streak_bonus, total_points, winner_correct, settled, created_at)
SELECT u.id, m.id, 2, 0, 3, 1, 0, 4, true, true, m.match_date - INTERVAL '2 days'
FROM users u, matches m WHERE u.username = 'rodrigo26' AND m.home_team = 'Argentina' AND m.away_team = 'Ecuador';

INSERT INTO predictions (user_id, match_id, home_score, away_score, points, early_bonus, streak_bonus, total_points, winner_correct, settled, created_at)
SELECT u.id, m.id, 1, 0, 5, 1, 2, 8, true, true, m.match_date - INTERVAL '2 days'
FROM users u, matches m WHERE u.username = 'rodrigo26' AND m.home_team = 'Argentina' AND m.away_team = 'México';

-- carlos predice algunos partidos
INSERT INTO predictions (user_id, match_id, home_score, away_score, points, early_bonus, streak_bonus, total_points, winner_correct, settled, created_at)
SELECT u.id, m.id, 2, 1, 5, 0, 0, 5, true, true, m.match_date - INTERVAL '10 hours'
FROM users u, matches m WHERE u.username = 'carlitos10' AND m.home_team = 'España' AND m.away_team = 'Brasil';

INSERT INTO predictions (user_id, match_id, home_score, away_score, points, early_bonus, streak_bonus, total_points, winner_correct, settled, created_at)
SELECT u.id, m.id, 3, 0, 3, 0, 0, 3, true, true, m.match_date - INTERVAL '5 hours'
FROM users u, matches m WHERE u.username = 'carlitos10' AND m.home_team = 'España' AND m.away_team = 'Croacia';

-- maria predice
INSERT INTO predictions (user_id, match_id, home_score, away_score, points, early_bonus, streak_bonus, total_points, winner_correct, settled, created_at)
SELECT u.id, m.id, 2, 2, 5, 1, 0, 6, true, true, m.match_date - INTERVAL '2 days'
FROM users u, matches m WHERE u.username = 'maria_gol' AND m.home_team = 'Brasil' AND m.away_team = 'Japón';

-- ── 6. LIGAS (salas de prueba) ────────────────────────────────────
INSERT INTO leagues (name, code, owner_id, created_at)
SELECT 'La Oficina Predice', 'OFIC26', u.id, NOW()
FROM users u WHERE u.username = 'rodrigo26';

INSERT INTO leagues (name, code, owner_id, created_at)
SELECT 'Amigos del Barrio', 'BARRI0', u.id, NOW()
FROM users u WHERE u.username = 'carlitos10';

-- ── 7. MIEMBROS DE LIGAS ─────────────────────────────────────────
-- Liga "La Oficina" — rodrigo es owner, se unen carlos, maria, juan
INSERT INTO league_members (league_id, user_id, joined_at)
SELECT l.id, u.id, NOW() FROM leagues l, users u
WHERE l.code = 'OFIC26' AND u.username IN ('rodrigo26', 'carlitos10', 'maria_gol', 'juanito7');

-- Liga "Amigos del Barrio" — carlos es owner, se une sofia
INSERT INTO league_members (league_id, user_id, joined_at)
SELECT l.id, u.id, NOW() FROM leagues l, users u
WHERE l.code = 'BARRI0' AND u.username IN ('carlitos10', 'sofi_fit');

-- ── 8. WALLETS con saldo inicial ─────────────────────────────────
INSERT INTO wallets (user_id, balance, currency)
SELECT id, 500.00, 'PEN' FROM users WHERE role = 'USER';

INSERT INTO wallets (user_id, balance, currency)
SELECT id, 1000.00, 'PEN' FROM users WHERE role = 'ADMIN';

-- ── 9. KYC aprobado para el admin y rodrigo ──────────────────────
INSERT INTO kyc_verifications (user_id, status, document_type, document_number, verified_at)
SELECT id, 'APPROVED', 'DNI', '12345678', NOW() FROM users WHERE username = 'admin';

INSERT INTO kyc_verifications (user_id, status, document_type, document_number, verified_at)
SELECT id, 'APPROVED', 'DNI', '87654321', NOW() FROM users WHERE username = 'rodrigo26';

-- KYC pendiente para los demás
INSERT INTO kyc_verifications (user_id, status, document_type, document_number)
SELECT id, 'PENDING', 'DNI', '11111111' FROM users WHERE username = 'carlitos10';
