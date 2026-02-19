package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.web.model.entity.*;
import com.fusi24.pangreksa.web.model.enumerate.MarriageEnum;
import com.fusi24.pangreksa.web.repo.HrPersonPtkpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PersonPtkpService {

    private final HrPersonPtkpRepository ptkpRepo;
    private final PtkpCalculatorService calculatorService;

    public PersonPtkpService(
            HrPersonPtkpRepository ptkpRepo,
            PtkpCalculatorService calculatorService
    ) {
        this.ptkpRepo = ptkpRepo;
        this.calculatorService = calculatorService;
    }

    @Transactional
    public HrPersonPtkp generateAndSavePtkp(
            HrPerson person,
            MarriageEnum marriageStatus,
            List<HrPersonTanggungan> tanggunganList,
            boolean jointIncome
    ) {

        // tutup PTKP lama
        ptkpRepo.findFirstByPersonAndValidToIsNull(person)
                .ifPresent(old -> {
                    old.setValidTo(LocalDate.now().minusDays(1));
                    ptkpRepo.save(old);
                });

        // default marriage kalau null
        if (marriageStatus == null) {
            marriageStatus = MarriageEnum.NO;   // default belum menikah = TK
        }
        int dependentCount = 0;

        if (tanggunganList != null && !tanggunganList.isEmpty()) {
            dependentCount = (int) tanggunganList.stream()
                    .filter(t -> Boolean.TRUE.equals(t.getStillDependent()))
                    .count();
        }

        HrPersonPtkp ptkp = calculatorService.calculateFromMaster(
                marriageStatus,
                dependentCount,
                jointIncome
        );

        ptkp.setPerson(person);
        return ptkpRepo.save(ptkp);
    }

}
