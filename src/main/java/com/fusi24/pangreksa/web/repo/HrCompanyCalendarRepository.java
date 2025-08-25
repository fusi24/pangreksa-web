package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrCompanyCalendar;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrCompanyCalendarRepository extends JpaRepository<HrCompanyCalendar, Long> {
    // find all calendars for a specific year and active true
    @EntityGraph(attributePaths = {"createdBy", "updatedBy"})
    List<HrCompanyCalendar> findAllByYearAndIsActiveTrueOrderByStartDateAsc(Integer year);
    @EntityGraph(attributePaths = {"createdBy", "updatedBy"})
    List<HrCompanyCalendar> findAllByYearOrderByStartDateAsc(Integer year);
}
