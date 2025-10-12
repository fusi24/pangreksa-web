package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrPositionLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HrPositionLevelRepository extends JpaRepository<HrPositionLevel, Long> {

    /**
     * Cari dengan keyword (case-insensitive) pada kolom position / positionDescription.
     * Jika keyword null/blank, query ini akan mengembalikan semua data (diurutkan).
     */
    @Query("""
        select p from HrPositionLevel p
        where (:kw is null or :kw = ''
               or lower(p.position) like lower(concat('%', :kw, '%'))
               or lower(p.position_description) like lower(concat('%', :kw, '%')))
        order by p.position asc
        """)
    List<HrPositionLevel> search(@Param("kw") String keyword);

    boolean existsByPositionIgnoreCase(String position);

    Optional<HrPositionLevel> findByPositionIgnoreCase(String position);
}