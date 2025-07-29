package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrPersonPosition;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HrPersonPositionRepository extends JpaRepository<HrPersonPosition, Long> {
    @EntityGraph(attributePaths = {"person","position","position.orgStructure","company","requestedBy"})
    List<HrPersonPosition> findByCompany(HrCompany company);
}
