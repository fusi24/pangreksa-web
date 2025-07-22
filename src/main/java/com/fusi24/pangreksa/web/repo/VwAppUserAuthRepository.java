package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.VwAppUserAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VwAppUserAuthRepository extends JpaRepository<VwAppUserAuth, Long> {

    // Cari user berdasarkan username
    List<VwAppUserAuth> findAllByIsActiveTrueAndUsernameOrderByResponsibilityAsc(String username);

    // Cari user berdasarkan email
    List<VwAppUserAuth> findAllByIsActiveTrueAndEmailOrderByResponsibilityAsc(String email);

    VwAppUserAuth findByIsActiveTrueAndUsernameAndResponsibilityAndPageId(String username, String responsibility, Long pageId);
}
