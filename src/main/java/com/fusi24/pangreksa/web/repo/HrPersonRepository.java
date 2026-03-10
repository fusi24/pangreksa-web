package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
    Page<HrPerson> findByKeyword(
            @Param("keyword") String keyword,
            Pageable pageable
    );


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


    @Query("""
        select p
        from HrPerson p
        where not exists (
            select 1
            from HrPersonPosition pp
            where pp.person = p
        )
        and (
            lower(p.firstName) like lower(concat('%', :keyword, '%'))
            or lower(p.lastName) like lower(concat('%', :keyword, '%'))
        )
        order by p.createdAt desc
    """)
    Page<HrPerson> findUnassignedPersonsByKeyword(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
        select p
        from HrPerson p
        join fetch p.personPosition pp
        join fetch pp.position pos
        join fetch pos.orgStructure org
        where org.id = :orgStructureId
        and p.id != :personId
    """)
    List<HrPerson> findCoworkersByOrgStructure(
            @Param("orgStructureId") Long orgStructureId,
            @Param("personId") Long personId,
            Pageable pageable
    );

    @Query("""
        select p
        from HrPerson p
        left join fetch p.personPosition pp
        left join fetch pp.position pos
        left join fetch pos.orgStructure org
        where p.id = :id
    """)
    HrPerson findPersonWithDetails(@Param("id") Long id);
}