package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrPerson;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrPersonRespository extends JpaRepository<HrPerson, Long> {
    @Query("SELECT h FROM HrPerson h WHERE h.id NOT IN (SELECT hp.person.id FROM HrPersonPosition hp)")
    List<HrPerson> findUnassignedPersons();

    @Query("SELECT h FROM HrPerson h WHERE " +
           "LOWER(h.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(h.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(h.middleName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(h.ktpNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<HrPerson> findByKeyword(String keyword, Pageable pageable);
}
