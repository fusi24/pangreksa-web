package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.FwSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface FwSystemRepository extends JpaRepository<FwSystem, UUID> {
    @Query("SELECT s FROM FwSystem s ORDER BY s.sortOrder ASC")
    List<FwSystem> findAllOrderBySortOrderAsc();

    @Query("SELECT s FROM FwSystem s WHERE s.id IN :ids ORDER BY s.sortOrder ASC")
    List<FwSystem> findByIdInOrderBySortOrderAsc(List<UUID> ids);

    List<FwSystem> findByKey(String key);

    List<FwSystem> findBySortOrderIn(Collection<Integer> sortOrders);

}

