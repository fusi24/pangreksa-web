package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.web.model.entity.*;
import com.fusi24.pangreksa.web.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {
    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final FwResponsibilitiesRepository responsibilitiesRepository;
    private final FwResponsibilitiesMenuRepository responsibilitiesMenuRepository;
    private final FwMenusRepository menusRepository;
    private final FwPagesRepository pagesRepository;
    private final FwAppUserRepository appUserRepository;

    public AdminService(FwResponsibilitiesRepository responsibilitiesRepository,
                        FwResponsibilitiesMenuRepository responsibilitiesMenuRepository,
                        FwMenusRepository menusRepository,
                        FwPagesRepository pagesRepository,
                        FwAppUserRepository appUserRepository) {
        this.responsibilitiesRepository = responsibilitiesRepository;
        this.responsibilitiesMenuRepository = responsibilitiesMenuRepository;
        this.menusRepository = menusRepository;
        this.pagesRepository = pagesRepository;
        this.appUserRepository = appUserRepository;
    }

    public List<FwPages> findAllPages(){
        return pagesRepository.findAll();
    }

    public void deleteResponsibilityMenu(FwResponsibilitiesMenu rm) {
        responsibilitiesMenuRepository.delete(rm);
        log.debug("Deleted FwResponsibilitiesMenu with ID: {}", rm.getId());
    }

    public List<FwResponsibilities> findActiveResponsibilities(){
        return responsibilitiesRepository.findByIsActiveTrue();
    }

    public List<FwResponsibilitiesMenu> findByResponsibilityMenu(FwResponsibilities responsibility) {
        return responsibilitiesMenuRepository.findByResponsibility(responsibility);
    }

    public FwAppUser findAppUserByUserId(String userId) {
        return appUserRepository.findByUsername(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
    }

    public void saveResponsibilityMenu(FwResponsibilitiesMenu rm, AppUserInfo user) {
        var appUser = this.findAppUserByUserId(user.getUserId().toString());

        log.debug("Data {} {} {} {} {} {}",
                rm.getMenu().getSortOrder(),
                rm.getMenu().getLabel(),
                rm.getMenu().getPage().getDescription().length(),
                rm.getMenu().getCanView(),
                rm.getMenu().getCanEdit(),
                rm.getMenu().getCanDelete());

        FwMenus fwMenus = null;

        // Save each menu item
        if (rm.getMenu().getId() == null) {
            rm.getMenu().setCreatedBy(appUser);
            rm.getMenu().setUpdatedBy(appUser);
            fwMenus = menusRepository.save(rm.getMenu());
            log.debug("New Menu saved with ID: {}", fwMenus.getId());
        } else {
            rm.getMenu().setUpdatedBy(appUser);
            menusRepository.saveAndFlush(rm.getMenu());
        }

        // Save the responsibilities menu item
        if (rm.getId() == null) {
            rm.setMenu(fwMenus);
            rm.setCreatedBy(appUser);
            rm.setUpdatedBy(appUser);
//            rm.setResponsibility(responsibilityDropdown.getValue());
            responsibilitiesMenuRepository.save(rm);
        } else {
            rm.setUpdatedBy(appUser);
            responsibilitiesMenuRepository.saveAndFlush(rm);
        }

        log.debug("Saved FwResponsibilitiesMenu with ID: {}", rm.getId());
    }
}
