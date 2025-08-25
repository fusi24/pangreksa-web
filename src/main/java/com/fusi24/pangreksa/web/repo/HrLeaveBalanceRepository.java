package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrLeaveAbsenceTypes;
import com.fusi24.pangreksa.web.model.entity.HrLeaveBalance;
import com.fusi24.pangreksa.web.model.entity.HrPerson;
import com.fusi24.pangreksa.web.model.enumerate.LeaveTypeEnum;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrLeaveBalanceRepository extends JpaRepository<HrLeaveBalance, Long> {

    List<HrLeaveBalance> findAllByYearAndCompany(int year, HrCompany company);

    HrLeaveBalance findByEmployeeAndYearAndLeaveAbsenceTypeAndCompany(HrPerson person, int year, HrLeaveAbsenceTypes leaveAbsenceTypes, HrCompany company);
    @EntityGraph(attributePaths = {"leaveAbsenceType"})
    List<HrLeaveBalance> findByEmployeeAndYearAndCompany(HrPerson person, int year, HrCompany company);

    long countByCompanyAndYear(HrCompany company, int year);
}
