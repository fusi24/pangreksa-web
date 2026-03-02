package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HrPersonRepository extends JpaRepository<HrPerson, Long> {

    @Query("""
    select p
    from HrPerson p
    where not exists (
        select 1    
        from HrPersonPosition pp
        where pp.person = p
    )
    order by p.createdAt desc
""")

    Page<HrPerson> findUnassignedPersons(Pageable pageable);

    @Query("""
    select p
    from HrPerson p
    where
        lower(p.firstName) like lower(concat('%', :keyword, '%'))
        or lower(p.middleName) like lower(concat('%', :keyword, '%'))
        or lower(p.lastName) like lower(concat('%', :keyword, '%'))
        or p.ktpNumber like concat('%', :keyword, '%')
""")
    Page<HrPerson> findByKeyword(String keyword, Pageable pageable);

    @Query("""
    select count(p)
    from HrPerson p
    where not exists (
        select 1
        from HrPersonPosition pp
        where pp.person = p
    )
""")
    long countUnassignedPersons();
}
