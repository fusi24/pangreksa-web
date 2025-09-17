package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrSalaryBaseLevel;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HrSalaryBaseLevelRepository extends JpaRepository<HrSalaryBaseLevel, Long> {
    // Find all salary levels by level code
    @EntityGraph(attributePaths = {"company"})
    List<HrSalaryBaseLevel> findByLevelCodeAndCompany(String levelCode, HrCompany company);

    // Optionally: find active salary level by code and date
    @EntityGraph(attributePaths = {"company"})
    List<HrSalaryBaseLevel> findByLevelCodeAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndCompany(
            String levelCode, LocalDate startDate, LocalDate endDate, HrCompany company
    );

    //Find all salary that end date is null order by level code asc
    @EntityGraph(attributePaths = {"company"})
    List<HrSalaryBaseLevel> findByCompanyAndEndDateIsNullOrderByLevelCodeAsc(HrCompany company);
    //find all salary  order by level code asc
    @EntityGraph(attributePaths = {"company"})
    List<HrSalaryBaseLevel> findByCompanyOrderByLevelCodeAsc(HrCompany company);

    // level aktif per tanggal (dan opsional company)
    @Query("""
      select b from HrSalaryBaseLevel b
      where b.startDate <= :today
        and (b.endDate is null or b.endDate >= :today)
        and (:companyId is null or b.company.id = :companyId)
      order by b.levelCode asc
    """)
    List<HrSalaryBaseLevel> findActive(@Param("today") LocalDate today,
                                           @Param("companyId") Long companyId);


}
