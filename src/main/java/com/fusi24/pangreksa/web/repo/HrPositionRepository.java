package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrOrgStructure;
import com.fusi24.pangreksa.web.model.entity.HrPosition;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrPositionRepository extends JpaRepository<HrPosition, Long> {

    // Existing method
    @EntityGraph(attributePaths = {"company","orgStructure"})
    List<HrPosition> findByCompanyAndOrgStructure(HrCompany company, HrOrgStructure orgStructure);

    // --- TAMBAHAN UNTUK SERVICE ---

    // Cari berdasarkan Name, Code, atau Org Structure Name
    @Query("SELECT p FROM HrPosition p " +
            "LEFT JOIN FETCH p.orgStructure o " +
            "LEFT JOIN FETCH p.reportsTo r " +
            "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(o.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<HrPosition> search(@Param("keyword") String keyword);

    // Cek duplikasi kode (Case Insensitive)
    boolean existsByCodeIgnoreCase(String code);
}