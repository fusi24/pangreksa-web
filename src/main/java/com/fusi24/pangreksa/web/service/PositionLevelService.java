package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.web.model.entity.FwAppUser;
import com.fusi24.pangreksa.web.repo.FwAppUserRepository;
import com.fusi24.pangreksa.web.model.entity.HrPositionLevel;
import com.fusi24.pangreksa.web.repo.HrPositionLevelRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class PositionLevelService {

    @PersistenceContext
    private EntityManager em;

    private final HrPositionLevelRepository repo;
    private final FwAppUserRepository appUserRepository;

    public PositionLevelService(HrPositionLevelRepository repo,
                              FwAppUserRepository appUserRepository) {
        this.repo = repo;
        this.appUserRepository = appUserRepository;
    }

    public FwAppUser findAppUserByUserId(String userId) {
        return appUserRepository.findByUsername(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
    }

    /**
     * Ambil semua atau cari dengan keyword (untuk tombol Populate).
     */
    @Transactional(readOnly = true)
    public List<HrPositionLevel> findAllOrSearch(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return repo.findAll().stream()
                    .sorted(Comparator.comparing(HrPositionLevel::getPosition, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }
        return repo.search(keyword.trim());
    }

    @Transactional
    public void saveAll(List<HrPositionLevel> rows, AppUserInfo actor) {
        var appUser = this.findAppUserByUserId(actor.getUserId().toString());
        LocalDateTime now = LocalDateTime.now();

        for (HrPositionLevel row : rows) {
            if (row == null) continue;

            // Normalisasi & basic validation ringan
            row.setPosition(safeTrim(row.getPosition()));
            row.setPosition_description(safeTrim(row.getPosition_description()));

            if (row.getPosition() == null || row.getPosition().isBlank()) {
                // bisa juga lempar exception custom; sementara skip baris kosong
                continue;
            }

            if (row.getId() == null) {
                // Baris baru
                row.setCreatedAt(now);
                row.setCreatedBy(appUser);
            }
            // Selalu update field update*
            row.setUpdatedAt(now);
            row.setUpdatedBy(appUser);
        }

        // Simpan sekaligus
        repo.saveAll(
                rows.stream()
                        .filter(Objects::nonNull)
                        .filter(p -> p.getPosition() != null && !p.getPosition().isBlank())
                        .toList()
        );
    }

    @Transactional
    public void deleteByIds(List<Long> ids, AppUserInfo actor) {
        if (ids == null || ids.isEmpty()) return;
        // kalau kamu butuh audit "siapa yang hapus", log saja di sini sebelum delete
        // contoh: log.info("Delete by {} -> {}", actor.getUserId(), ids);

        // hard delete
        repo.deleteAllByIdInBatch(ids);
    }

    @Transactional
    public HrPositionLevel create(String position, String description, AppUserInfo actor) {
        var appUser = this.findAppUserByUserId(actor.getUserId().toString());
        String pos = safeTrim(position);
        String desc = safeTrim(description);

        if (pos == null || pos.isBlank()) {
            throw new IllegalArgumentException("Position wajib diisi.");
        }
        if (repo.existsByPositionIgnoreCase(pos)) {
            throw new IllegalArgumentException("Position sudah ada: " + pos);
        }

        LocalDateTime now = LocalDateTime.now();
        HrPositionLevel e = new HrPositionLevel();
        e.setPosition(pos);
        e.setPosition_description(desc);
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        e.setCreatedBy(appUser);
        e.setUpdatedBy(appUser);

        return repo.save(e);
    }

    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }
}

