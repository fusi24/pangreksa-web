package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrTaxBracket;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface HrTaxBracketRepository extends CrudRepository<HrTaxBracket, Long> {

    public List<HrTaxBracket> findAllByOrderByMinIncomeAsc();
}
