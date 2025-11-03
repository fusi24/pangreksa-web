package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.web.model.entity.*;
import com.fusi24.pangreksa.web.model.enumerate.LeaveStatusEnum;
import com.fusi24.pangreksa.web.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
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
    private final HrLeaveAbsenceTypesRepository leaveAbsenceTypesRepository;

    public LeaveService(HrPersonPositionRepository personPositionRepository, HrLeaveBalanceRepository leaveBalanceRepository,FwAppUserRepository appUserRepository,
                        FwSystemRepository systemRepository, HrLeaveGenerationLogRepository leaveGenerationLogRepository, HrLeaveApplicationRepository leaveApplicationRepository,
                        HrLeaveAbsenceTypesRepository leaveAbsenceTypesRepository) {
        this.personPositionRepository = personPositionRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.appUserRepository = appUserRepository;
        this.systemRepository = systemRepository;
        this.leaveGenerationLogRepository = leaveGenerationLogRepository;
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.leaveAbsenceTypesRepository = leaveAbsenceTypesRepository;

        this.getLeaveAbsenceTypesList();
    }

    public List<HrLeaveAbsenceTypes> findAllLeaveAbsenceTypesList() {
        return leaveAbsenceTypesRepository.findAllByOrderBySortOrderAsc();
    }

    public List<HrLeaveAbsenceTypes> getLeaveAbsenceTypesList() {
     return leaveAbsenceTypesRepository.findAllByIsEnableTrueOrderBySortOrderAsc();
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
//        List<LeaveTypeEnum> leaveTypes = Arrays.asList(LeaveTypeEnum.values());
        for (HrPersonPosition personPosition : personPositionList) {
            log.debug("Generating leave balance for person: {} in company: {} for year: {}",
                    personPosition.getPerson().getFirstName() + " " + personPosition.getPerson().getLastName(), company.getName(), year);

            List<HrLeaveAbsenceTypes> leaveAbsenceTypesList = leaveAbsenceTypesRepository.findAllByIsEnableTrueOrderBySortOrderAsc();

            for (HrLeaveAbsenceTypes leaveAbsenceType : leaveAbsenceTypesList) {
                HrPerson person = personPosition.getPerson();
                HrLeaveBalance leaveBalance = HrLeaveBalance.builder()
                        .employee(person)
                        .company(company)
                        .year(year)
                        .leaveAbsenceType(leaveAbsenceType)
                        .generationLog(generationLog)
                        .allocatedDays(leaveAbsenceType.getMaxAllowedDays())
                        .usedDays(0)
                        .build();

                leaveBalance.setCreatedAt(LocalDateTime.now());
                leaveBalance.setUpdatedAt(LocalDateTime.now());
                leaveBalance.setCreatedBy(appUser);
                leaveBalance.setUpdatedBy(appUser);

                leaveBalance = leaveBalanceRepository.save(leaveBalance);

                log.debug("Saving leave balance for person: {}, leave type: {}, allocated days: {}",
                        person.getFirstName() + " " + person.getLastName(), leaveAbsenceType.getLeaveAbsenceType(), leaveBalance.getAllocatedDays());

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
                application.getLeaveAbsenceType().getLabel(), application.getStartDate(), application.getEndDate());

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

    public HrLeaveBalance getLeaveBalance(AppUserInfo appUser, int year, HrLeaveAbsenceTypes leaveAbsenceType) {
        FwAppUser user = this.findAppUserByUserId(appUser.getUserId().toString());
        return leaveBalanceRepository.findByEmployeeAndYearAndLeaveAbsenceTypeAndCompany(
                user.getPerson(),
                year,
                leaveAbsenceType,
                user.getCompany()
        );
    }

    public List<HrLeaveBalance> getAllLeaveBalance(AppUserInfo appUser, int year) {
        FwAppUser user = this.findAppUserByUserId(appUser.getUserId().toString());
        return leaveBalanceRepository.findByEmployeeAndYearAndCompany(
                user.getPerson(),
                year,
                user.getCompany()
        );
    }

    public Boolean updateLeaveBalance(HrLeaveApplication application, AppUserInfo appUserInfo) {
        FwAppUser appUser = this.findAppUserByUserId(appUserInfo.getUserId().toString());

        HrLeaveBalance leaveBalance = leaveBalanceRepository.findByEmployeeAndYearAndLeaveAbsenceTypeAndCompany(
                application.getEmployee(),
                application.getStartDate().getYear(),
                application.getLeaveAbsenceType(),
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

    public HrLeaveAbsenceTypes saveLeaveAbsenceType(HrLeaveAbsenceTypes leaveAbsenceType, AppUserInfo appUserInfo) {
        FwAppUser appUser = this.findAppUserByUserId(appUserInfo.getUserId().toString());

        if (leaveAbsenceType.getId() == null) {
            leaveAbsenceType.setCreatedBy(appUser);
            leaveAbsenceType.setCreatedAt(LocalDateTime.now());
            leaveAbsenceType.setUpdatedBy(appUser);
            leaveAbsenceType.setUpdatedAt(LocalDateTime.now());
        } else {
            leaveAbsenceType.setUpdatedBy(appUser);
            leaveAbsenceType.setUpdatedAt(LocalDateTime.now());
        }

        return leaveAbsenceTypesRepository.save(leaveAbsenceType);
    }

    public void deleteLeaveAbsenceType(Long id, AppUserInfo appUserInfo) {
        leaveAbsenceTypesRepository.deleteById(id);
    }
}
