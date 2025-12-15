package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrPerson;
import com.fusi24.pangreksa.web.model.entity.HrPersonTanggungan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HrPersonTanggunganRepository
        extends JpaRepository<HrPersonTanggungan, Long> {

    List<HrPersonTanggungan> findByPerson(HrPerson person);
}
