package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.web.model.entity.HrPersonPtkp;
import com.fusi24.pangreksa.web.model.enumerate.MarriageEnum;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class PtkpCalculatorService {

    // PTKP rules (berlaku sejak 2016)
    private static final BigDecimal BASE_TK = BigDecimal.valueOf(54_000_000);
    private static final BigDecimal DEPENDENT = BigDecimal.valueOf(4_500_000);
    private static final BigDecimal BASE_KI = BigDecimal.valueOf(112_500_000);

    /**
     * @param marriageStatus status perkawinan
     * @param dependentCount jumlah tanggungan (maks 3)
     * @param jointIncome true jika penghasilan suami-istri digabung (K/I)
     */
    public HrPersonPtkp calculate(
            MarriageEnum marriageStatus,
            int dependentCount,
            boolean jointIncome
    ) {
        HrPersonPtkp ptkp = new HrPersonPtkp();

        // maksimal 3 tanggungan
        int dep = Math.min(Math.max(dependentCount, 0), 3);

        BigDecimal amount;
        String code;

        if (jointIncome) {
            // K/I/0
            amount = BASE_KI;
            code = "K/I/0";
        } else {
            amount = BASE_TK;

            if (marriageStatus != null && marriageStatus.name().equalsIgnoreCase("YES")) {
                amount = amount.add(DEPENDENT);
                code = "K/" + dep;
            } else {
                code = "TK/" + dep;
            }

            amount = amount.add(DEPENDENT.multiply(BigDecimal.valueOf(dep)));
        }

        ptkp.setMarriageStatus(marriageStatus);
        ptkp.setPtkpCode(code);
        ptkp.setPtkpAmount(amount);
        ptkp.setValidFrom(LocalDate.now());
        ptkp.setValidTo(null); // aktif sampai diganti

        return ptkp;
    }
}
