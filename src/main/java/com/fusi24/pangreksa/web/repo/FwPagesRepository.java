package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.FwPages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FwPagesRepository extends JpaRepository<FwPages, Long> {

    // Cari berdasarkan roleName (exact match)
    List<FwPages> findByRoleName(String roleName);

    // Cari berdasarkan roleName mengandung keyword (case-insensitive)
    List<FwPages> findByRoleNameContainingIgnoreCase(String keyword);

    // Cari berdasarkan pageUrl
    List<FwPages> findByPageUrl(String pageUrl);
}

