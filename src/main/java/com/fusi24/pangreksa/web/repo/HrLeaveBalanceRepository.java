package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrLeaveBalance;
import com.fusi24.pangreksa.web.model.entity.HrPerson;
import com.fusi24.pangreksa.web.model.enumerate.LeaveTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrLeaveBalanceRepository extends JpaRepository<HrLeaveBalance, Long> {

    List<HrLeaveBalance> findAllByYearAndCompany(int year, HrCompany company);
    HrLeaveBalance findByEmployeeAndYearAndLeaveTypeAndCompany(HrPerson person, int year, LeaveTypeEnum leaveType, HrCompany company);

    long countByCompanyAndYear(HrCompany company, int year);
}
