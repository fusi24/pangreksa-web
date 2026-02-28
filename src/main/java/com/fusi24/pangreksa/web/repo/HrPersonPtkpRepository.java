package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrPerson;
import com.fusi24.pangreksa.web.model.entity.HrPersonPtkp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface HrPersonPtkpRepository extends JpaRepository<HrPersonPtkp, Long> {

    Optional<HrPersonPtkp> findFirstByPersonAndValidToIsNull(HrPerson person);

    @Query("""
        select p
        from HrPersonPtkp p
        where p.person.id = :personId
          and p.validTo is null
        order by p.validFrom desc nulls last
    """)
        Optional<HrPersonPtkp> findCurrentByPersonId(@Param("personId") Long personId);
}
