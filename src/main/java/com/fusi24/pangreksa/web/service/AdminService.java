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
    private final FwAppuserRespRepository appuserRespRepository;

    public AdminService(FwResponsibilitiesRepository responsibilitiesRepository,
                        FwResponsibilitiesMenuRepository responsibilitiesMenuRepository,
                        FwMenusRepository menusRepository,
                        FwPagesRepository pagesRepository,
                        FwAppUserRepository appUserRepository,
                        FwAppuserRespRepository appuserRespRepository) {
        this.responsibilitiesRepository = responsibilitiesRepository;
        this.responsibilitiesMenuRepository = responsibilitiesMenuRepository;
        this.menusRepository = menusRepository;
        this.pagesRepository = pagesRepository;
        this.appUserRepository = appUserRepository;
        this.appuserRespRepository = appuserRespRepository;
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
        return responsibilitiesMenuRepository.findByResponsibility(responsibility)
            .stream()
            .sorted((a, b) -> Integer.compare(
                a.getMenu().getSortOrder(),
                b.getMenu().getSortOrder()
            ))
            .toList();
    }

    public FwAppUser findAppUserByUserId(String userId) {
        return appUserRepository.findByUsername(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
    }

    public FwAppUser findAppUserById(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("User not found with ID: " + id));
    }

    public List<FwAppUser> findAllAppUsersByKeyword(String keyword) {
        return appUserRepository.findAllByUsernameOrNicknameContainingIgnoreCaseOrderByUsernameAsc(keyword);
    }

    public List<FwAppUser> findAllAppUsers() {
        return appUserRepository.findAllByOrderByUsernameAsc();
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

    public FwAppUser saveAppUser(FwAppUser user, AppUserInfo appUserInfo) {
        var appUser = this.findAppUserByUserId(appUserInfo.getUserId().toString());

        if(user.getId() == null) {
            user.setCreatedBy(appUser);
            user.setUpdatedBy(appUser);
        } else {
            user.setUpdatedBy(appUser);
        }

        return appUserRepository.save(user);
    }

    public List<FwAppuserResp> findAppUserRespByUser(FwAppUser appUser) {
        return appuserRespRepository.findByAppuser(appUser);
    }

    public void saveAppUserResp(FwAppuserResp appuserResp, AppUserInfo appUserInfo) {
        var appUser = this.findAppUserByUserId(appUserInfo.getUserId().toString());

        if(appuserResp.getId() == null) {
            appuserResp.setCreatedBy(appUser);
            appuserResp.setUpdatedBy(appUser);
        } else {
            appuserResp.setUpdatedBy(appUser);
        }

        appuserRespRepository.save(appuserResp);
    }

    public void deleteAppUserResp(FwAppuserResp appuserResp) {
        appuserRespRepository.delete(appuserResp);
        log.debug("Deleted FwAppuserResp with ID: {}", appuserResp.getId());
    }
}
