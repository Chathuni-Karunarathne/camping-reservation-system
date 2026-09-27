package camp_res_system.service;

import camp_res_system.model.Campsite;
import camp_res_system.repository.CampsiteRepository;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;

public final class CampsiteService {
    private final CampsiteRepository sites;
    private final Session session;

    public CampsiteService(CampsiteRepository sites, Session session) {
        this.sites = sites;
        this.session = session;
    }

    public List<Campsite> search(String query, boolean includeInactive) {
        session.requireUser();
        if (includeInactive) session.requireAdmin();
        try {
            return sites.search(query.trim(), includeInactive);
        } catch (SQLException e) {
            throw new AppException("error.database", e);
        }
    }

    public void save(Campsite site, boolean create) {
        session.requireAdmin();
        if (site.id() == null
                || !site.id().matches("[A-Za-z0-9_-]{1,30}")
                || site.name().isBlank()
                || site.name().length() > 100
                || site.province().isBlank()
                || site.province().length() > 100
                || site.description().length() > 2000
                || site.rateCents() < 1
                || site.rateCents() > 100_000_000) throw new AppException("error.campsite");
        try {
            sites.save(
                    new Campsite(
                            site.id().trim(),
                            site.name().trim(),
                            site.province().trim(),
                            site.description().trim(),
                            site.rateCents(),
                            site.active()),
                    create);
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) throw new AppException("error.duplicateSite");
            throw new AppException("error.database", e);
        }
    }

    public void archive(String id) {
        session.requireAdmin();
        try {
            sites.remove(id);
        } catch (SQLException e) {
            throw new AppException("error.database", e);
        }
    }

    public void loadDemo() {
        session.requireAdmin();
        try (var stream = CampsiteService.class.getResourceAsStream("/db/demo-campsites.tsv")) {
            if (stream == null) throw new IOException("Missing demo resource");
            for (String line :
                    new String(stream.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] row = line.split("\t");
                if (sites.find(row[0]).isEmpty())
                    save(
                            new Campsite(
                                    row[0], row[1], row[2], row[3], Long.parseLong(row[4]), true),
                            true);
            }
        } catch (IOException | SQLException e) {
            throw new AppException("error.database", e);
        }
    }
}
