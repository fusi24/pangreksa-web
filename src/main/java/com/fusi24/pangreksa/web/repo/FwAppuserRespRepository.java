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

    // === Projection untuk Grid ===
    public interface GridRow {
        Long getId();
        Long getAppuserId();
        Long getResponsibilityId();
        String getUsername();
        String getNickname();
        String getRole();     // description/label
        Boolean getIsActive();
    }

    // === Projection untuk ComboBox Options (User/Role) ===
    public interface OptionRow {
        Long getId();
        String getLabel();
    }

    @org.springframework.data.jpa.repository.Query(value = """
        SELECT
            r.id AS id,
            r.appuser_id AS appuserId,
            r.responsibility_id AS responsibilityId,
            u.username AS username,
            u.nickname AS nickname,
            COALESCE(resp.description, resp.label) AS role,
            COALESCE(r.is_active, true) AS isActive
        FROM fw_appuser_resp r
        JOIN fw_appuser u ON u.id = r.appuser_id
        JOIN fw_responsibilities resp ON resp.id = r.responsibility_id
        WHERE (:kw IS NULL OR :kw = '' OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :kw, '%')))
        ORDER BY LOWER(u.nickname), r.id
        """, nativeQuery = true)
    List<GridRow> findGridRowsByNickname(String kw);

    @org.springframework.data.jpa.repository.Query(value = """
        SELECT u.id AS id, CONCAT(u.nickname, ' (', u.username, ')') AS label
        FROM fw_appuser u
        WHERE (u.is_active IS TRUE OR u.is_active IS NULL)
          AND (:kw IS NULL OR :kw = '' OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :kw, '%')))
        ORDER BY LOWER(u.nickname)
        LIMIT 50
        """, nativeQuery = true)
    List<OptionRow> searchUserOptions(String kw);

    @org.springframework.data.jpa.repository.Query(value = """
        SELECT r.id AS id, COALESCE(r.description, r.label) AS label
        FROM fw_responsibilities r
        ORDER BY COALESCE(r.is_active, true) DESC, COALESCE(r.sort_order, 999999),
                 LOWER(COALESCE(r.description, r.label))
        """, nativeQuery = true)
    List<OptionRow> findAllResponsibilityOptions();

    // Jika kebijakan: 1 user = 1 role, method helper ini memudahkan:
    java.util.Optional<FwAppuserResp> findFirstByAppuserOrderByIdAsc(FwAppUser appuser);

}