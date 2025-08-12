package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrLeaveApplication;
import com.fusi24.pangreksa.web.model.entity.HrPerson;
import com.fusi24.pangreksa.web.model.enumerate.LeaveStatusEnum;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HrLeaveApplicationRepository extends JpaRepository<HrLeaveApplication, Long> {
    // Find by referenceId
    //List<HrLeaveApplication> findByReferenceId(Long referenceId);

    @EntityGraph(attributePaths = {"employee", "submittedTo", "approvedBy"})
    List<HrLeaveApplication> findByEmployeeAndSubmittedAtBetweenOrderBySubmittedAtDesc(
            HrPerson employee,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    @EntityGraph(attributePaths = {"employee", "submittedTo", "approvedBy"})
    List<HrLeaveApplication> findBySubmittedToAndSubmittedAtBetweenAndStatusInOrderBySubmittedAtDesc(
            HrPerson submittedTo,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            List<LeaveStatusEnum> statuses
    );
}
