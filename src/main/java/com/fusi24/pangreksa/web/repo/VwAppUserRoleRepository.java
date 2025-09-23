package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.VwAppUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VwAppUserRoleRepository extends JpaRepository<VwAppUserRole, Long> {

    // Cari user berdasarkan username
    List<VwAppUserRole> findByUsername(String username);

    // Cari user berdasarkan email
    List<VwAppUserRole> findByEmail(String email);
}
