package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.FwMenus;
import com.fusi24.pangreksa.web.model.entity.VwAppUserAuth;
import com.fusi24.pangreksa.web.repo.FwMenusRepository;
import com.fusi24.pangreksa.web.repo.FwPagesRepository;
import com.fusi24.pangreksa.web.repo.VwAppUserAuthRepository;
import org.springframework.stereotype.Service;

@Service
public class CommonService {
    private final FwMenusRepository menusRepository;
    private final VwAppUserAuthRepository appUserAuthRepository;

    public CommonService(FwMenusRepository menusRepository, VwAppUserAuthRepository appUserAuthRepository) {
        this.menusRepository = menusRepository;
        this.appUserAuthRepository = appUserAuthRepository;

    }

    public Authorization getAuthorization(AppUserInfo user, String responsibility, Long pageId) {
        VwAppUserAuth appUserAuth =  appUserAuthRepository.findByIsActiveTrueAndUsernameAndResponsibilityAndPageId(user.getUserId().toString(), responsibility, pageId);

        if (appUserAuth == null) {
            return new Authorization(false, false, false, false);
        } else {
            FwMenus fwMenus = menusRepository.findById(appUserAuth.getMenuId())
                    .orElseThrow(() -> new IllegalStateException("Page not found with ID: " + appUserAuth.getMenuId()));

            return new Authorization(fwMenus.getCanView(),
                    fwMenus.getCanCreate(),
                    fwMenus.getCanEdit(),
                    fwMenus.getCanDelete());
        }
    }
}
