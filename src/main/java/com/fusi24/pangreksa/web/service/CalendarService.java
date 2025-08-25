package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.web.model.entity.FwAppUser;
import com.fusi24.pangreksa.web.model.entity.HrCompanyCalendar;
import com.fusi24.pangreksa.web.repo.FwAppUserRepository;
import com.fusi24.pangreksa.web.repo.HrCompanyCalendarRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CalendarService {
    private static final Logger log = LoggerFactory.getLogger(CalendarService.class);
    private final HrCompanyCalendarRepository companyCalendarRepository;
    private final FwAppUserRepository appUserRepository;

    public CalendarService(HrCompanyCalendarRepository companyCalendarRepository, FwAppUserRepository appUserRepository) {
        this.companyCalendarRepository = companyCalendarRepository;
        this.appUserRepository = appUserRepository;

    }

    private FwAppUser findAppUserByUserId(String userId) {
        return appUserRepository.findByUsername(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
    }

    public List<HrCompanyCalendar> getCompanyCalendarsByYear(int selectedYear) {
        return companyCalendarRepository.findAllByYearOrderByStartDateAsc(selectedYear);
    }

    public void deleteCompanyCalendar(HrCompanyCalendar calendar) {
        this.companyCalendarRepository.delete(calendar);
    }

    public HrCompanyCalendar saveCompanyCalendar(HrCompanyCalendar calendar, AppUserInfo appUserInfo) {
        FwAppUser appUser = this.findAppUserByUserId(appUserInfo.getUserId().toString());

        if (calendar.getId() != null) {
            calendar.setCreatedBy(appUser);
            calendar.setUpdatedBy(appUser);
            calendar.setCreatedAt(LocalDateTime.now());
            calendar.setUpdatedAt(LocalDateTime.now());
        } else {
            calendar.setUpdatedBy(appUser);
            calendar.setUpdatedAt(LocalDateTime.now());
        }

        return this.companyCalendarRepository.save(calendar);
    }
}
