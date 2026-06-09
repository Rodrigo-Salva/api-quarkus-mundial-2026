package com.mundial2026.predictions.tournaments.repository;

import com.mundial2026.predictions.tournaments.entity.Tournament;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class TournamentRepository implements PanacheRepository<Tournament> {

    public Optional<Tournament> findByCode(String code) {
        return find("code", code).firstResultOptional();
    }
}
