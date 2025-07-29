package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrCompany;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrCompanyRepository extends JpaRepository<HrCompany, Long> {
    // Find by referenceId
    //List<HrCompany> findByReferenceId(Long referenceId);

    @Query("SELECT c FROM HrCompany c WHERE " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    @EntityGraph(attributePaths = {"parent"})
    List<HrCompany> findByKeyword(String keyword, Pageable pageable);
}
