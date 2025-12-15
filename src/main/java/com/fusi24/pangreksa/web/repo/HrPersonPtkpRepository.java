package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrPerson;
import com.fusi24.pangreksa.web.model.entity.HrPersonPtkp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HrPersonPtkpRepository extends JpaRepository<HrPersonPtkp, Long> {

    Optional<HrPersonPtkp> findFirstByPersonAndValidToIsNull(HrPerson person);
}
