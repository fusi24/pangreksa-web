package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.web.model.dto.UserLevelRow;

public class RowMapperUtil {
    public static UserLevelRow map(Object[] r) {
        // index mengikuti SELECT di repo
        Long userId = r[0] == null ? null : ((Number) r[0]).longValue();
        String username = (String) r[1];
        String email = (String) r[2];
        String nickname = (String) r[3];
        String firstName = (String) r[4];
        String lastName = (String) r[5];
        Long companyId = r[6] == null ? null : ((Number) r[6]).longValue();
        Long idHsel = r[7] == null ? null : ((Number) r[7]).longValue();
        Long idHsbl = r[8] == null ? null : ((Number) r[8]).longValue();
        String levelCode = (String) r[9];
        java.math.BigDecimal baseSalary = (java.math.BigDecimal) r[10];

        String fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
        String displayName = fullName.isBlank() ? username : fullName;
        if (nickname != null && !nickname.isBlank()) {
            displayName = displayName + " (" + nickname + ")";
        }

        UserLevelRow dto = new UserLevelRow();
        dto.setUserId(userId);
        dto.setDisplayName(displayName);
        dto.setCompanyId(companyId);
        dto.setSelectedBaseLevelId(idHsbl);
        dto.setSelectedLevelCode(levelCode);
        dto.setSelectedBaseSalary(baseSalary);
        dto.setDirty(false);
        return dto;
    }
}