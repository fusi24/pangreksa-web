package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.web.model.entity.FwAppUser;
import com.fusi24.pangreksa.web.model.entity.HrPosition;
import com.fusi24.pangreksa.web.repo.FwAppUserRepository;
import com.fusi24.pangreksa.web.repo.HrPositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class HrPositionService {

    private final HrPositionRepository repo;
    private final FwAppUserRepository appUserRepository;

    public HrPositionService(HrPositionRepository repo,
                             FwAppUserRepository appUserRepository) {
        this.repo = repo;
        this.appUserRepository = appUserRepository;
    }

    // Helper untuk mengambil user yang sedang login
    public FwAppUser findAppUserByUserId(String userId) {
        return appUserRepository.findByUsername(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
    }

    /**
     * Ambil semua data atau cari berdasarkan keyword.
     * Data diurutkan berdasarkan Nama Org Structure, kemudian Nama Posisi.
     */
    @Transactional(readOnly = true)
    public List<HrPosition> findAllOrSearch(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            // Jika tidak ada search, ambil semua dan sorting di Java
            return repo.findAll().stream()
                    .sorted(Comparator.comparing((HrPosition p) ->
                                    p.getOrgStructure() != null ? p.getOrgStructure().getName() : "")
                            .thenComparing(HrPosition::getName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }
        // Jika ada search, gunakan query custom di Repo
        return repo.search(keyword.trim());
    }

    /**
     * Simpan List Posisi (Create atau Update)
     */
    @Transactional
    public void saveAll(List<HrPosition> rows, AppUserInfo actor) {
        var appUser = this.findAppUserByUserId(actor.getUserId().toString());
        LocalDateTime now = LocalDateTime.now();

        for (HrPosition row : rows) {
            if (row == null) continue;

            // 1. Normalisasi Input (Trim spasi)
            row.setName(safeTrim(row.getName()));
            row.setCode(safeTrim(row.getCode()));
            row.setNotes(safeTrim(row.getNotes()));

            // 2. Validasi Wajib Diisi
            if (row.getName() == null || row.getName().isBlank()) {
                continue; // Skip baris jika nama kosong
            }

            // 3. Default Values
            if (row.getIsManagerial() == null) {
                row.setIsManagerial(false);
            }

            // 4. Set Audit Fields
            if (row.getId() == null) {
                // --- LOGIKA CREATE ---
                row.setCreatedAt(now);
                row.setCreatedBy(appUser);

                // Validasi Unik Kode (Hanya saat Create baru)
                if (row.getCode() != null && !row.getCode().isBlank()) {
                    if (repo.existsByCodeIgnoreCase(row.getCode())) {
                        throw new IllegalArgumentException("Kode posisi '" + row.getCode() + "' sudah digunakan.");
                    }
                }
            }
            // --- LOGIKA UPDATE (Selalu jalan) ---
            row.setUpdatedAt(now);
            row.setUpdatedBy(appUser);

            // Catatan:
            // - Relasi 'orgStructure' (org_structure_id) sudah dihandle otomatis via object HrOrgStructure di entity.
            // - Relasi 'reportsTo' (reports_to) sudah dihandle otomatis via object HrPosition di entity.
            // - Relasi 'company' (company_id) sebaiknya diset di UI atau di sini jika ada logic khusus per user.
        }

        // Simpan data yang valid (Name tidak kosong)
        repo.saveAll(
                rows.stream()
                        .filter(Objects::nonNull)
                        .filter(p -> p.getName() != null && !p.getName().isBlank())
                        .toList()
        );
    }

    /**
     * Hapus Data Berdasarkan List ID
     */
    @Transactional
    public void deleteByIds(List<Long> ids, AppUserInfo actor) {
        if (ids == null || ids.isEmpty()) return;

        // Hapus batch (lebih efisien daripada loop delete)
        repo.deleteAllByIdInBatch(ids);
    }

    // Utility untuk trim string dan handle null
    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }
}