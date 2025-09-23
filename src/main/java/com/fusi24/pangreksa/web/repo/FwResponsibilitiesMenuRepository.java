package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.FwMenus;
import com.fusi24.pangreksa.web.model.entity.FwResponsibilities;
import com.fusi24.pangreksa.web.model.entity.FwResponsibilitiesMenu;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FwResponsibilitiesMenuRepository extends JpaRepository<FwResponsibilitiesMenu, Long> {

    @EntityGraph(attributePaths = {"menu","menu.page"})
    // Cari semua menu yang terhubung ke responsibility tertentu
    List<FwResponsibilitiesMenu> findByResponsibility(FwResponsibilities responsibility);

    // Cari semua responsibility yang terhubung ke menu tertentu
    List<FwResponsibilitiesMenu> findByMenu(FwMenus menu);

    // Cek apakah hubungan responsibility-menu sudah ada
    boolean existsByResponsibilityAndMenu(FwResponsibilities responsibility, FwMenus menu);

    // Cari satu hubungan spesifik
    Optional<FwResponsibilitiesMenu> findByResponsibilityAndMenu(FwResponsibilities responsibility, FwMenus menu);

    // Cari semua yang aktif untuk sebuah responsibility
    List<FwResponsibilitiesMenu> findByResponsibilityAndIsActiveTrue(FwResponsibilities responsibility);
}
