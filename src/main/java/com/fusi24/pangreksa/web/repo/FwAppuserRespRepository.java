package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.FwAppUser;
import com.fusi24.pangreksa.web.model.entity.FwAppuserResp;
import com.fusi24.pangreksa.web.model.entity.FwResponsibilities;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FwAppuserRespRepository extends JpaRepository<FwAppuserResp, Long> {

    // Cari semua responsibility untuk user tertentu
    @EntityGraph(attributePaths = {"appuser","responsibility"})
    List<FwAppuserResp> findByAppuser(FwAppUser appuser);

    // Cari semua user yang memiliki responsibility tertentu
    List<FwAppuserResp> findByResponsibility(FwResponsibilities responsibility);

    // Cari satu kombinasi user dan responsibility
    Optional<FwAppuserResp> findByAppuserAndResponsibility(FwAppUser appuser, FwResponsibilities responsibility);

    // Cek apakah user sudah memiliki responsibility tertentu
    boolean existsByAppuserAndResponsibility(FwAppUser appuser, FwResponsibilities responsibility);
}