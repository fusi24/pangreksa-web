package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrLeaveAbsenceTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrLeaveAbsenceTypesRepository extends JpaRepository<HrLeaveAbsenceTypes, Long> {
    // Find by referenceId
    //List<HrLeaveAbsenceTypes> findByReferenceId(Long referenceId);
    List<HrLeaveAbsenceTypes> findAllByOrderBySortOrderAsc();
    List<HrLeaveAbsenceTypes> findAllByIsEnableTrueOrderBySortOrderAsc();
}
