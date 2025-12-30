package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrLeaveApplication;
import com.fusi24.pangreksa.web.model.entity.HrPerson;
import com.fusi24.pangreksa.web.model.enumerate.LeaveStatusEnum;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HrLeaveApplicationRepository extends JpaRepository<HrLeaveApplication, Long> {
    // Find by referenceId
    //List<HrLeaveApplication> findByReferenceId(Long referenceId);

    @EntityGraph(attributePaths = {"employee", "submittedTo", "approvedBy","leaveAbsenceType"})
    List<HrLeaveApplication> findByEmployeeAndSubmittedAtBetweenOrderBySubmittedAtDesc(
            HrPerson employee,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    @EntityGraph(attributePaths = {"employee", "submittedTo", "approvedBy","leaveAbsenceType"})
    List<HrLeaveApplication> findBySubmittedToAndSubmittedAtBetweenAndStatusInOrderBySubmittedAtDesc(
            HrPerson submittedTo,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            List<LeaveStatusEnum> statuses
    );

    boolean existsByEmployeeAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatusIn(
            HrPerson employee,
            LocalDate onOrBefore,
            LocalDate onOrAfter,
            List<LeaveStatusEnum> statuses
    );

    @Query("""
       select coalesce(sum(l.totalDays), 0)
       from HrLeaveApplication l
       where l.employee.id = :personId
         and l.status in :statuses
         and l.startDate >= :startDate
         and l.startDate < :endDate
       """)
    long sumLeaveDaysByPersonAndPeriodAndStatuses(@Param("personId") Long personId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate,
                                                  @Param("statuses") List<LeaveStatusEnum> statuses);

}
