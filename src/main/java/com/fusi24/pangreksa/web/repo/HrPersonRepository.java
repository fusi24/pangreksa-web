package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrPerson;
import com.fusi24.pangreksa.web.model.entity.HrPersonPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface HrPersonRepository extends JpaRepository<HrPerson, Long> {

    @Query("""
        select distinct p
        from HrPerson p
        where exists (
            select 1
            from HrPersonPosition pp
            where pp.person = p
              and pp.company = :company
              and pp.startDate <= :today
              and (pp.endDate is null or pp.endDate >= :today)
        )
        order by p.firstName asc, p.lastName asc
    """)
    List<HrPerson> findActiveByCompanyViaPosition(@Param("company") HrCompany company,
                                                  @Param("today") LocalDate today);
}
