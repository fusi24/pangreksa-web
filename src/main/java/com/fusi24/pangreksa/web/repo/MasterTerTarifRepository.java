package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.MasterTerTarif;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface MasterTerTarifRepository extends JpaRepository<MasterTerTarif, Long> {

    @Query("""
        select t
        from MasterTerTarif t
        where t.aktif = true
          and t.jenisTer = :jenisTer
          and :bruto between t.brutoMin and t.brutoMax
        order by t.urutan asc
    """)
    Optional<MasterTerTarif> findEffectiveTarif(@Param("jenisTer") String jenisTer,
                                                @Param("bruto") BigDecimal bruto);
}