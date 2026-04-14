package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.MasterTer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MasterTerRepository extends JpaRepository<MasterTer, Long> {
    Optional<MasterTer> findFirstByMasterPtkpIdAndAktifTrue(Long masterPtkpId);
}