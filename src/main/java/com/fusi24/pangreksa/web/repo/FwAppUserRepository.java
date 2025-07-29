package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.FwAppUser;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FwAppUserRepository extends JpaRepository<FwAppUser, Long> {

    @EntityGraph(attributePaths = {"person","company"})
    @Query(
            "SELECT u FROM FwAppUser u WHERE " +
                    "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "ORDER BY u.username ASC"
    )
    List<FwAppUser> findAllByUsernameOrNicknameContainingIgnoreCaseOrderByUsernameAsc(String keyword);

    @EntityGraph(attributePaths = {"person","company"})
    List<FwAppUser> findAllByOrderByUsernameAsc();

    @Override
    @EntityGraph(attributePaths = {"person","company"})
    Optional<FwAppUser> findById(@NotNull Long id);

    // Cari user berdasarkan username
    Optional<FwAppUser> findByUsername(String username);

    // Cari user berdasarkan email
    Optional<FwAppUser> findByEmail(String email);

    // Cek apakah username sudah digunakan
    boolean existsByUsername(String username);

    // Cek apakah email sudah digunakan
    boolean existsByEmail(String email);
}
