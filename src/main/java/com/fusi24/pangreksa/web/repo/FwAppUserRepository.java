package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.FwAppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FwAppUserRepository extends JpaRepository<FwAppUser, Long> {

    // Cari user berdasarkan username
    Optional<FwAppUser> findByUsername(String username);

    // Cari user berdasarkan email
    Optional<FwAppUser> findByEmail(String email);

    // Cek apakah username sudah digunakan
    boolean existsByUsername(String username);

    // Cek apakah email sudah digunakan
    boolean existsByEmail(String email);
}
