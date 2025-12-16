package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.web.model.entity.HrPersonPtkp;
import com.fusi24.pangreksa.web.model.entity.MasterPtkp;
import com.fusi24.pangreksa.web.model.enumerate.MarriageEnum;
import com.fusi24.pangreksa.web.repo.MasterPtkpRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class PtkpCalculatorService {

    private final MasterPtkpRepository masterPtkpRepository;

    public PtkpCalculatorService(MasterPtkpRepository masterPtkpRepository) {
        this.masterPtkpRepository = masterPtkpRepository;
    }

    public HrPersonPtkp calculateFromMaster(
            MarriageEnum marriageStatus,
            int dependentCount,
            boolean jointIncome
    ) {

        // 1. Normalisasi tanggungan (max 3)
        int dep = Math.min(Math.max(dependentCount, 0), 3);

        // 2. Generate KODE PTKP
        String kodePtkp;
        if (jointIncome) {
            kodePtkp = "K/I/0";
        } else if (marriageStatus != null &&
                !marriageStatus.name().equalsIgnoreCase("SINGLE") &&
                !marriageStatus.name().equalsIgnoreCase("TK")) {
            kodePtkp = "K/" + dep;
        } else {
            kodePtkp = "TK/" + dep;
        }

        // 3. Ambil dari MASTER
        MasterPtkp master = masterPtkpRepository
                .findByKodePtkpAndAktifTrue(kodePtkp)
                .orElseThrow(() ->
                        new IllegalStateException("Master PTKP tidak ditemukan: " + kodePtkp)
                );

        // 4. Map ke entity karyawan_ptkp
        HrPersonPtkp ptkp = new HrPersonPtkp();
        ptkp.setMarriageStatus(marriageStatus);
        ptkp.setPtkpCode(master.getKodePtkp());
        ptkp.setPtkpAmount(master.getNominal());
        ptkp.setValidFrom(LocalDate.now());
        ptkp.setValidTo(null);

        return ptkp;
    }
}
