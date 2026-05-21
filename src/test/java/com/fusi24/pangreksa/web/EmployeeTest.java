package com.fusi24.pangreksa.web;

import com.pangreksa.service.model.entity.FwAppUser;
import com.pangreksa.service.model.entity.HrPersonPosition;
import com.pangreksa.service.model.repo.FwAppUserRepository;
import com.pangreksa.service.service.PersonService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
public class EmployeeTest {

    private static final long COMPANY_ID = 1L;

    @Autowired
    private PersonService personService;

    @Autowired
    private FwAppUserRepository fwAppUserRepository;

    private FwAppUser user;

    @BeforeEach
    public void getUser(){
        this.user =  fwAppUserRepository.findByUsername("medisa").orElse(new FwAppUser());
    }

    @Test
    void getAllPersonPosition() {
        personService.getCompanyById(COMPANY_ID);
        List<HrPersonPosition> positionList = personService.getPersonHasPositionInCompany();
        assertThat(positionList).isNotEmpty();

        for( HrPersonPosition position : positionList) {
            log.info("Position: {}, Person: {} {}, Start Date: {}, End Date: {}, Is Primary: {}, Is Acting: {}, Organization: {}",
                    position.getPosition().getName(),
                    position.getPerson().getFirstName(),
                    position.getPerson().getLastName(),
                    position.getStartDate(),
                    position.getEndDate(),
                    position.getIsPrimary(),
                    position.getIsActing(),
                    position.getPosition().getOrgStructure().getName());
        }
    }
}
