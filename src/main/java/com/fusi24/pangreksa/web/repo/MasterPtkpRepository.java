package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.MasterPtkp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MasterPtkpRepository extends JpaRepository<MasterPtkp, Long> {

    Optional<MasterPtkp> findByKodePtkpAndAktifTrue(String kodePtkp);
}