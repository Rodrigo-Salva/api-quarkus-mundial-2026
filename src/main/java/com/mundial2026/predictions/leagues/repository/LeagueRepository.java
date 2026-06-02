package com.mundial2026.predictions.leagues.repository;

import com.mundial2026.predictions.leagues.entity.League;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class LeagueRepository implements PanacheRepository<League> {

    public Optional<League> findByCode(String code) {
        return find("code", code).firstResultOptional();
    }
}
