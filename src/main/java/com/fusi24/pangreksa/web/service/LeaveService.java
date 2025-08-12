package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.web.model.entity.*;
import com.fusi24.pangreksa.web.model.enumerate.LeaveStatusEnum;
import com.fusi24.pangreksa.web.model.enumerate.LeaveTypeEnum;
import com.fusi24.pangreksa.web.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LeaveService {
    private static final Logger log = LoggerFactory.getLogger(LeaveService.class);
    private final HrPersonPositionRepository personPositionRepository;
    private final HrLeaveBalanceRepository leaveBalanceRepository;
    private final FwAppUserRepository appUserRepository;
    private final FwSystemRepository systemRepository;
    private final HrLeaveGenerationLogRepository leaveGenerationLogRepository;
    private final HrLeaveApplicationRepository leaveApplicationRepository;

    private Map<String, Integer> leaveTypeDaysMap;

    public LeaveService(HrPersonPositionRepository personPositionRepository, HrLeaveBalanceRepository leaveBalanceRepository,FwAppUserRepository appUserRepository,
                        FwSystemRepository systemRepository, HrLeaveGenerationLogRepository leaveGenerationLogRepository, HrLeaveApplicationRepository leaveApplicationRepository) {
        this.personPositionRepository = personPositionRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.appUserRepository = appUserRepository;
        this.systemRepository = systemRepository;
        this.leaveGenerationLogRepository = leaveGenerationLogRepository;
        this.leaveApplicationRepository = leaveApplicationRepository;

        setLeaveTypeDaysMap();
    }

    public List<HrLeaveGenerationLog> getLeaveGenerationLogs(HrCompany company) {
        return leaveGenerationLogRepository.findByCompanyOrderByYearDesc(company);
    }

    public long countPersonPerCompany(HrCompany company, int year) {
        return personPositionRepository.countActivePersonsByCompanyAndYear(company, year);
    }

    public long countLeaveBalanceRowPerCompany(HrCompany company, int year) {
        return leaveBalanceRepository.countByCompanyAndYear(company,year);
    }

    private FwAppUser findAppUserByUserId(String userId) {
        return appUserRepository.findByUsername(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
    }

    private void setLeaveTypeDaysMap(){
        this.leaveTypeDaysMap = Map.of(
                LeaveTypeEnum.CUTI.toString(), systemRepository.findById(UUID.fromString("85270560-2051-431e-9ce4-5c8e4a7373e0")).orElseThrow().getIntVal(),
                LeaveTypeEnum.CUTI_KHUSUS.toString(), systemRepository.findById(UUID.fromString("b675d92c-fd4c-45fc-b075-49ec13e23e00")).orElseThrow().getIntVal(),
                LeaveTypeEnum.IZIN.toString(), systemRepository.findById(UUID.fromString("ac02cdb9-190c-44ac-b7ff-bd502e86d6e0")).orElseThrow().getIntVal(),
                LeaveTypeEnum.SAKIT.toString(), systemRepository.findById(UUID.fromString("85fa7773-dc20-4b8d-95cd-ae5d4c8fe143")).orElseThrow().getIntVal(),
                LeaveTypeEnum.MELAHIRKAN.toString(), systemRepository.findById(UUID.fromString("455c35d9-f299-4af1-9ca4-c5ec3bb8d2c4")).orElseThrow().getIntVal(),
                LeaveTypeEnum.LIBUR_NASIONAL.toString(), systemRepository.findById(UUID.fromString("526efa2b-74df-42a9-8c4b-cb45280c284f")).orElseThrow().getIntVal(),
                LeaveTypeEnum.PERJALANAN_DINAS.toString(), systemRepository.findById(UUID.fromString("bdcef9a1-9c9f-488f-a35e-5589123e0611")).orElseThrow().getIntVal()
        );
    }

    public void generateLeaveBalanceData(HrCompany company, int year, AppUserInfo appUserInfo ) {
        var appUser = this.findAppUserByUserId(appUserInfo.getUserId().toString());

        HrLeaveGenerationLog generationLog = HrLeaveGenerationLog.builder()
                .company(company)
                .year(year)
                .build();
        generationLog.setCreatedBy(appUser);
        generationLog.setUpdatedBy(appUser);
        generationLog.setCreatedAt(LocalDateTime.now());
        generationLog.setUpdatedAt(LocalDateTime.now());

        generationLog = leaveGenerationLogRepository.saveAndFlush(generationLog);
        int dataGenerated = 0;

        List<HrPersonPosition> personPositionList = personPositionRepository.findActivePersonsNotInLeaveBalanceByCompanyAndYear(company, year);
        //get list of leave types
        List<LeaveTypeEnum> leaveTypes = Arrays.asList(LeaveTypeEnum.values());
        for (HrPersonPosition personPosition : personPositionList) {
            log.debug("Generating leave balance for person: {} in company: {} for year: {}",
                    personPosition.getPerson().getFirstName() + " " + personPosition.getPerson().getLastName(), company.getName(), year);

            for (LeaveTypeEnum leaveType : leaveTypes) {
                HrPerson person = personPosition.getPerson();
                HrLeaveBalance leaveBalance = HrLeaveBalance.builder()
                        .employee(person)
                        .company(company)
                        .year(year)
                        .leaveType(leaveType)
                        .generationLog(generationLog)
                        .allocatedDays(leaveTypeDaysMap.get(leaveType.toString()))
                        .usedDays(0)
                        .build();

                leaveBalance.setCreatedAt(LocalDateTime.now());
                leaveBalance.setUpdatedAt(LocalDateTime.now());
                leaveBalance.setCreatedBy(appUser);
                leaveBalance.setUpdatedBy(appUser);

                leaveBalance = leaveBalanceRepository.save(leaveBalance);

                log.debug("Saving leave balance for person: {}, leave type: {}, allocated days: {}",
                        person.getFirstName() + " " + person.getLastName(), leaveType, leaveBalance.getAllocatedDays());

                if (leaveBalance.getId() != null) {
                    dataGenerated++;

                    generationLog.setDataGenerated(dataGenerated);
                    generationLog = leaveGenerationLogRepository.saveAndFlush(generationLog);
                }
            }
        }
    }

    public HrLeaveApplication saveApplication(HrLeaveApplication application, AppUserInfo appUserInfo) {
        FwAppUser appUser = this.findAppUserByUserId(appUserInfo.getUserId().toString());

        switch (application.getStatus().toString()) {
            case "APPROVED":
                if (application.getId() != null) {
                    application.setUpdatedBy(appUser);
                    application.setUpdatedAt(LocalDateTime.now());
                    application.setApprovedBy(appUser.getPerson());
                    application.setApprovedAt(LocalDateTime.now());
                }
                break;
            case "REJECTED":
                if (application.getId() != null) {
                    application.setUpdatedBy(appUser);
                    application.setUpdatedAt(LocalDateTime.now());
                }
                break;
            default:
                if (application.getId() == null) {
                    application.setCreatedBy(appUser);
                    application.setCreatedAt(LocalDateTime.now());
                    application.setUpdatedBy(appUser);
                    application.setUpdatedAt(LocalDateTime.now());
                    application.setCompany(appUser.getCompany());
                    application.setEmployee(appUser.getPerson());
                    application.setSubmittedAt(LocalDateTime.now());
                } else {
                    application.setUpdatedBy(appUser);
                    application.setUpdatedAt(LocalDateTime.now());
                }

                // Calculate total days
                if (application.getStartDate() != null && application.getEndDate() != null) {
                    application.setTotalDays((int) (application.getEndDate().toEpochDay() - application.getStartDate().toEpochDay()) + 1);
                } else {
                    application.setTotalDays(0);
                }
        }

        log.debug("Saving leave application for person: {}, leave type: {}, start date: {}, end date: {}",
                application.getEmployee().getFirstName() + " " + application.getEmployee().getLastName(),
                application.getLeaveType(), application.getStartDate(), application.getEndDate());

        return leaveApplicationRepository.save(application);
    }

    public List<HrLeaveApplication> getLeaveApplicationsByEmployee(AppUserInfo appUser) {
        FwAppUser user = this.findAppUserByUserId(appUser.getUserId().toString());
        int months = systemRepository.findById(UUID.fromString("3ef1d277-7a3a-40b1-98d5-0df3e89dcd96")).orElseThrow().getIntVal();
        // create current date minus months
        LocalDateTime currentDateMinusMonths = LocalDateTime.now().minusMonths(months);
        return leaveApplicationRepository.findByEmployeeAndSubmittedAtBetweenOrderBySubmittedAtDesc(user.getPerson(), currentDateMinusMonths, LocalDateTime.now());
    }

    public List<HrLeaveApplication> getLeaveApplicationForApproval(AppUserInfo appUser) {
        FwAppUser user = this.findAppUserByUserId(appUser.getUserId().toString());
        int months = systemRepository.findById(UUID.fromString("3ef1d277-7a3a-40b1-98d5-0df3e89dcd96")).orElseThrow().getIntVal();
        // create current date minus months
        LocalDateTime currentDateMinusMonths = LocalDateTime.now().minusMonths(months);
        return leaveApplicationRepository.findBySubmittedToAndSubmittedAtBetweenAndStatusInOrderBySubmittedAtDesc(
                user.getPerson(),
                currentDateMinusMonths,
                LocalDateTime.now(),
                List.of(LeaveStatusEnum.SUBMITTED)
        );
    }

    public HrLeaveBalance getLeaveBalance(AppUserInfo appUser, int year, LeaveTypeEnum leaveType) {
        FwAppUser user = this.findAppUserByUserId(appUser.getUserId().toString());
        return leaveBalanceRepository.findByEmployeeAndYearAndLeaveTypeAndCompany(
                user.getPerson(),
                year,
                leaveType,
                user.getCompany()
        );
    }

    public Boolean updateLeaveBalance(HrLeaveApplication application, AppUserInfo appUserInfo) {
        FwAppUser appUser = this.findAppUserByUserId(appUserInfo.getUserId().toString());

        HrLeaveBalance leaveBalance = leaveBalanceRepository.findByEmployeeAndYearAndLeaveTypeAndCompany(
                application.getEmployee(),
                application.getStartDate().getYear(),
                application.getLeaveType(),
                application.getCompany()
        );

        if (application.getStatus() == LeaveStatusEnum.APPROVED) {
            leaveBalance.setUsedDays(leaveBalance.getUsedDays() + application.getTotalDays());
            leaveBalance.setUpdatedBy(appUser);
            leaveBalance.setUpdatedAt(LocalDateTime.now());
            leaveBalanceRepository.save(leaveBalance);
        }

        return true;
    }
}
