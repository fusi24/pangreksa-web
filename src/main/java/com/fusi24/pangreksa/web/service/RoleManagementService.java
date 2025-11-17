package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.web.model.entity.FwAppUser;
import com.fusi24.pangreksa.web.model.entity.FwAppuserResp;
import com.fusi24.pangreksa.web.model.entity.FwResponsibilities;
import com.fusi24.pangreksa.web.repo.FwAppUserRepository;
import com.fusi24.pangreksa.web.repo.FwAppuserRespRepository;
import com.fusi24.pangreksa.web.repo.FwAppuserRespRepository.GridRow;
import com.fusi24.pangreksa.web.repo.FwAppuserRespRepository.OptionRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoleManagementService {

    @PersistenceContext
    private EntityManager em;

    private final FwAppuserRespRepository appuserRespRepo;
    private final FwAppUserRepository appUserRepository; // mengikuti pola contoh

    public RoleManagementService(FwAppuserRespRepository appuserRespRepo,
                                 FwAppUserRepository appUserRepository) {
        this.appuserRespRepo = appuserRespRepo;
        this.appUserRepository = appUserRepository;
    }

    // === DTO utk batch save dari Grid ===
    public static class UpdateRowDto {
        public Long id;
        public Long responsibilityId;
        public Boolean isActive;
    }

    /** Populate grid (projection native) */
    public List<GridRow> findRoleRows(String keyword) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return appuserRespRepo.findGridRowsByNickname(kw);
    }

    /** Options: combo user (searchable) */
    public List<OptionRow> searchUserOptions(String keyword) {
        String kw = (keyword == null || keyword.isBlank()) ? "" : keyword.trim();
        return appuserRespRepo.searchUserOptions(kw);
    }

    /** Options: combo role */
    public List<OptionRow> getResponsibilityOptions() {
        return appuserRespRepo.findAllResponsibilityOptions();
    }

    /** Helper: resolve actor → FwAppUser sesuai pola contoh */
    private FwAppUser resolveActor(AppUserInfo actor) {
        // di contoh kamu: findAppUserByUserId(actor.getUserId().toString())
        return appUserRepository.findByUsername(actor.getUserId().toString())
                .orElseThrow(() -> new IllegalStateException("User not found: " + actor.getUserId()));
    }

    /**
     * Add mapping (kebijakan default: 1 user = 1 role → upsert).
     * Kalau kamu ingin multi-role, ganti logikanya jadi always-insert (aku bisa siapkan varian kedua).
     */
    @Transactional
    public void addMapping(Long appuserId, Long responsibilityId, AppUserInfo actor) {
        FwAppUser appActor = resolveActor(actor);

        var userRef = em.getReference(FwAppUser.class, appuserId);
        var roleRef = em.getReference(FwResponsibilities.class, responsibilityId);


        // 1. Cek apakah mapping (user + role) SUDAH ADA menggunakan method baru
        boolean alreadyExists = appuserRespRepo.existsByAppuserAndResponsibility(userRef, roleRef);

        if (alreadyExists) {
            // 2. Lempar error jika duplikat (Sesuai permintaan Anda)
            // Error ini akan ditangkap oleh RoleManagementView dan ditampilkan sebagai notif galat
            throw new IllegalArgumentException("Duplikat: Pengguna ini sudah memiliki role tersebut.");

        } else {
            // 3. Buat baru jika belum ada
            var e = new FwAppuserResp();
            e.setAppuser(userRef);
            e.setResponsibility(roleRef);
            e.setIsActive(Boolean.TRUE); // default aktif
            e.setCreatedBy(appActor);
            e.setUpdatedBy(appActor);
            appuserRespRepo.save(e);
        }
    }

    /** Batch update dari Grid: ubah role & status, audit updatedBy = actor */
    @Transactional
    public void saveBatch(List<UpdateRowDto> updates, AppUserInfo actor) {
        if (updates == null || updates.isEmpty()) return;

        FwAppUser appActor = resolveActor(actor);

        Map<Long, UpdateRowDto> map = updates.stream()
                .filter(u -> u != null && u.id != null)
                .collect(Collectors.toMap(u -> u.id, u -> u, (a, b) -> b, LinkedHashMap::new));

        var entities = appuserRespRepo.findAllById(map.keySet());
        for (var e : entities) {
            var u = map.get(e.getId());
            if (u.responsibilityId != null) {
                e.setResponsibility(em.getReference(FwResponsibilities.class, u.responsibilityId));
            }
            if (u.isActive != null) {
                e.setIsActive(u.isActive);
            }
            e.setUpdatedBy(appActor);
        }
        appuserRespRepo.saveAll(entities);
    }
}
