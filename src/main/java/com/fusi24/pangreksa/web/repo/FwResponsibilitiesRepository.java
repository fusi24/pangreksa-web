package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.FwResponsibilities;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FwResponsibilitiesRepository extends JpaRepository<FwResponsibilities, Long> {

    // Cari responsibility berdasarkan label (exact)
    Optional<FwResponsibilities> findByIsActiveTrueAndLabel(String label);

    // Cari responsibility berdasarkan label yang mengandung kata kunci (case-insensitive)
    List<FwResponsibilities> findByLabelContainingIgnoreCase(String keyword);

    // Cek apakah label responsibility sudah ada
    boolean existsByLabel(String label);

    // Cari semua responsibility aktif
    List<FwResponsibilities> findByIsActiveTrue();

    // Urutkan berdasarkan kolom sort_order naik
    List<FwResponsibilities> findAllByIsActiveTrueOrderBySortOrderAsc();
}
