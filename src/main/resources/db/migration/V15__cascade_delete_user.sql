-- Permite eliminar usuarios sin violar FK constraints
-- Todas las tablas que referencian users(id) pasan a ON DELETE CASCADE

ALTER TABLE predictions
    DROP CONSTRAINT IF EXISTS predictions_user_id_fkey,
    ADD CONSTRAINT predictions_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE leagues
    DROP CONSTRAINT IF EXISTS leagues_owner_id_fkey,
    ADD CONSTRAINT leagues_owner_id_fkey
        FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE league_members
    DROP CONSTRAINT IF EXISTS league_members_user_id_fkey,
    ADD CONSTRAINT league_members_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE kyc_verifications
    DROP CONSTRAINT IF EXISTS kyc_verifications_user_id_fkey,
    ADD CONSTRAINT kyc_verifications_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE gambling_limits
    DROP CONSTRAINT IF EXISTS gambling_limits_user_id_fkey,
    ADD CONSTRAINT gambling_limits_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE self_exclusions
    DROP CONSTRAINT IF EXISTS self_exclusions_user_id_fkey,
    ADD CONSTRAINT self_exclusions_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE wallets
    DROP CONSTRAINT IF EXISTS wallets_user_id_fkey,
    ADD CONSTRAINT wallets_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE bets
    DROP CONSTRAINT IF EXISTS bets_user_id_fkey,
    ADD CONSTRAINT bets_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE accumulator_bets
    DROP CONSTRAINT IF EXISTS accumulator_bets_user_id_fkey,
    ADD CONSTRAINT accumulator_bets_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
