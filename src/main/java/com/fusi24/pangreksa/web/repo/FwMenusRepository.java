package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.FwMenus;
import com.fusi24.pangreksa.web.model.entity.FwPages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FwMenusRepository extends JpaRepository<FwMenus, Long> {

    // Cari menu berdasarkan label persis
    List<FwMenus> findByLabel(String label);

    // Cari menu berdasarkan label mengandung keyword (case-insensitive)
    List<FwMenus> findByLabelContainingIgnoreCase(String keyword);

    // Cari semua menu berdasarkan page tertentu
    List<FwMenus> findByPage(FwPages page);

    // Cari dan urutkan berdasarkan sort_order
    List<FwMenus> findAllByOrderBySortOrderAsc();
}
