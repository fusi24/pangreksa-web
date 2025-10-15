package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.FwMenuGroup;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface FwMenuGroupRepo extends CrudRepository<FwMenuGroup, Long> {

    public List<FwMenuGroup> findByIsActiveTrue();
}
