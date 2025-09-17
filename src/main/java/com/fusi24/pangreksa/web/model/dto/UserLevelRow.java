package com.fusi24.pangreksa.web.model.dto;

import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
public class UserLevelRow {
    private Long userId;
    private String displayName;
    private Long companyId;
    private Long selectedBaseLevelId;
    private String selectedLevelCode;
    private BigDecimal selectedBaseSalary;

    // flag internal untuk tahu perlu upsert
    private boolean dirty;

}
