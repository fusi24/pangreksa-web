package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrPerson;
import com.fusi24.pangreksa.web.model.entity.HrPersonPosition;
import com.fusi24.pangreksa.web.model.entity.HrPosition;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HrPersonPositionRepository extends JpaRepository<HrPersonPosition, Long> {
    @Query("""
    SELECT p
    FROM HrPersonPosition p
    WHERE p.company = :company
      AND p.person = :person
      AND p.startDate <= :currentDate
      AND (p.endDate IS NULL OR p.endDate >= :currentDate)
    """)
    @EntityGraph(attributePaths = {"person","position","position.orgStructure","company","requestedBy"})
    HrPersonPosition findCurrentPositionsByCompanyAndPerson(
            @Param("company") HrCompany company,
            @Param("person") HrPerson person,
            @Param("currentDate") LocalDate currentDate
    );


    @EntityGraph(attributePaths = {"person","position","position.orgStructure","company","requestedBy"})
    List<HrPersonPosition> findByCompany(HrCompany company);

    @EntityGraph(attributePaths = {"person","position","position.orgStructure","company","requestedBy"})
    List<HrPersonPosition> findByCompanyAndPosition(HrCompany company, HrPosition position);

    @Query("""
    SELECT COUNT(DISTINCT p.person.id)
    FROM HrPersonPosition p
    WHERE p.company = :company
      AND (
            YEAR(p.startDate) <= :year AND 
            (p.endDate IS NULL OR YEAR(p.endDate) >= :year)
      )
    """)
    long countActivePersonsByCompanyAndYear(@Param("company") HrCompany company, @Param("year") int year);

    @Query("""
    SELECT DISTINCT p
    FROM HrPersonPosition p
    WHERE p.company = :company
      AND (
            YEAR(p.startDate) <= :year AND 
            (p.endDate IS NULL OR YEAR(p.endDate) >= :year)
      )
    """)
    @EntityGraph(attributePaths = {"person"})
    List<HrPersonPosition> findByActivePersonsByCompanyAndYear(@Param("company") HrCompany company, @Param("year") int year);

    @Query("""
    SELECT DISTINCT p
    FROM HrPersonPosition p
    WHERE p.company = :company
      AND (
            YEAR(p.startDate) <= :year AND 
            (p.endDate IS NULL OR YEAR(p.endDate) >= :year)
      )
      AND NOT EXISTS (
          SELECT 1
          FROM HrLeaveBalance lb
          WHERE lb.employee.id = p.person.id
            AND lb.company = :company
            AND lb.year = :year
      )
    """)
    @EntityGraph(attributePaths = {"person"})
    List<HrPersonPosition> findActivePersonsNotInLeaveBalanceByCompanyAndYear(
            @Param("company") HrCompany company,
            @Param("year") int year
    );
}
