package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HrPositionRepository extends JpaRepository<HrPosition, Long> {
    // Find by referenceId
    //List<HrPosition> findByReferenceId(Long referenceId);
}
