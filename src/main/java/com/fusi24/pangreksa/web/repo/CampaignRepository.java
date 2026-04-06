package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    /**
     * Mengambil campaign yang aktif secara manual DAN berada dalam rentang tanggal hari ini.
     * Diurutkan berdasarkan prioritas terkecil (ASC) kemudian tanggal buat terbaru.
     */
    @Query("SELECT c FROM Campaign c " +
            "WHERE c.isActive = true " +
            "AND CURRENT_DATE BETWEEN c.startDate AND c.endDate " +
            "ORDER BY c.priority ASC, c.createdAt DESC")
    List<Campaign> findActiveCampaignsForDashboard();

    /**
     * Mengambil semua data untuk halaman manajemen (Admin/HR).
     */
    List<Campaign> findAllByOrderByCreatedAtDesc();
}