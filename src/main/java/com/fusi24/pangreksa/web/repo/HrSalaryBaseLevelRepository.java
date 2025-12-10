package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrSalaryBaseLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;


import java.time.LocalDate;
import java.util.List;

@Repository
public interface HrSalaryBaseLevelRepository extends JpaRepository<HrSalaryBaseLevel, Long> {

    // ====== ✅ REQUIRED BY PayrollService (error Anda) ======

    @EntityGraph(attributePaths = "company")
    List<HrSalaryBaseLevel> findByCompanyOrderByLevelCodeAsc(HrCompany company);

    @EntityGraph(attributePaths = "company")
    List<HrSalaryBaseLevel> findByCompanyAndEndDateIsNullOrderByLevelCodeAsc(HrCompany company);

    @EntityGraph(attributePaths = "company")
    List<HrSalaryBaseLevel> findByCompanyAndIsActiveTrueOrderByLevelCodeAsc(HrCompany company);


    // ====== ✅ REQUIRED BY SalaryLevelService ======

    @Query("""
           SELECT s FROM HrSalaryBaseLevel s
           WHERE s.company.id = :companyId
             AND s.startDate <= :today
             AND (s.endDate IS NULL OR s.endDate >= :today)
           ORDER BY s.startDate DESC
           """)
    List<HrSalaryBaseLevel> findActive(@Param("today") LocalDate today,
                                       @Param("companyId") Long companyId);


    // ====== ✅ REQUIRED BY SalaryBaseLevelService (versioning) ======

    @Query("""
           SELECT s FROM HrSalaryBaseLevel s
           WHERE s.company = :company
             AND s.startDate <= :today
             AND (s.endDate IS NULL OR s.endDate >= :today)
           ORDER BY s.startDate DESC
           """)
    List<HrSalaryBaseLevel> findActiveList(@Param("company") HrCompany company,
                                           @Param("today") LocalDate today);

    default HrSalaryBaseLevel findActiveVersion(HrCompany company) {
        List<HrSalaryBaseLevel> list = findActiveList(company, LocalDate.now());
        return list.isEmpty() ? null : list.get(0);
    }


    /**
     * Max sequence dari format LEVEL-XXX-YYYY.
     * Contoh: LEVEL-001-2025
     */
    @Query(value = """
            SELECT MAX(CAST(SUBSTRING(level_code FROM 7 FOR 3) AS INT))
            FROM hr_salary_base_level
            WHERE company_id = :companyId
              AND SUBSTRING(level_code FROM 11 FOR 4) = CAST(:year AS TEXT)
            """, nativeQuery = true)
    Integer findMaxSequenceByYear(@Param("companyId") Long companyId,
                                  @Param("year") int year);
}
