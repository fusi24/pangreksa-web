package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.web.model.Responsibility;
import com.fusi24.pangreksa.web.model.entity.VwAppUserAuth;
import com.fusi24.pangreksa.web.repo.VwAppUserAuthRepository;
import com.vaadin.flow.server.menu.MenuEntry;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AppUserAuthService {

    @Autowired
    private VwAppUserAuthRepository appUserAuthRepository;

    public List<Responsibility> getAllResponsibilitiesFromUsername(String username)  {
        List<VwAppUserAuth> appUserAuthList = appUserAuthRepository.findAllByIsActiveTrueAndUsernameOrderByResponsibilityAsc(username);

        return getResponsibility(appUserAuthList);
    }

    public List<Responsibility> getAllResponsibilitiesFromEmail(String email)  {
        List<VwAppUserAuth> appUserAuthList = appUserAuthRepository.findAllByIsActiveTrueAndEmailOrderByResponsibilityAsc(email);

        return getResponsibility(appUserAuthList);
    }

    private @NotNull List<Responsibility> getResponsibility(List<VwAppUserAuth> appUserAuthList){
        List<Responsibility> responsibilityList = new ArrayList<Responsibility>();

        // Get Distinct Responsibilities
        Set<String> distinctResponsibilities = appUserAuthList.stream()
                .map(VwAppUserAuth::getResponsibility)
                .collect(Collectors.toSet());

        distinctResponsibilities.forEach(r -> {
            Responsibility responsibility = new Responsibility(r);

            appUserAuthList.stream()
                    .filter(auth -> auth.getResponsibility().equals(r))
                    .forEach(auth -> {
                        responsibility.addMenu(new MenuEntry(
                                auth.getUrl(),
                                auth.getLabel(),
                                auth.getSortOrder(),
                                auth.getPageIcon(), null
                        ));
                        responsibility.addGroupMenu(auth.getGroupName(), new MenuEntry(
                                auth.getUrl(),
                                auth.getLabel(),
                                auth.getSortOrder(),
                                auth.getPageIcon(), null
                        ));
                    });
            responsibilityList.add(responsibility);
        });

        return responsibilityList;
    }
}
