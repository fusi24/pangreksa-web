package com.fusi24.pangreksa.web.repo;

import com.fusi24.pangreksa.web.model.entity.HrSalaryEmployeeLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface HrSalaryEmployeeLevelRepository extends JpaRepository<HrSalaryEmployeeLevel, Long> {

    Optional<HrSalaryEmployeeLevel> findByAppUser_Id(Long appUserId);

    Optional<HrSalaryEmployeeLevel> findByAppUserId(Long appUserId);

    /** Closed projection untuk grid */
    interface UserLevelProjection {
        Long getUserId();
        String getDisplayName();
        Long getCompanyId();
        Long getSelectedBaseLevelId();
        String getSelectedLevelCode();
        BigDecimal getSelectedBaseSalary();
    }

    @Query(value = """
        select
            u.id                                         as userId,
            -- displayName = first+last (fallback username) + optional (nickname)
            (
              case
                when btrim(coalesce(p.first_name,'') || ' ' || coalesce(p.last_name,'')) = '' then u.username
                else btrim(coalesce(p.first_name,'') || ' ' || coalesce(p.last_name,''))
              end
            ) ||
            case when coalesce(nullif(u.nickname,''),'') <> '' then ' (' || u.nickname || ')' else '' end
                                                      as displayName,
            u.company_id                                 as companyId,
            m.id_salary                                  as selectedBaseLevelId,
            b.level_code                                 as selectedLevelCode,
            b.base_salary                                as selectedBaseSalary
        from fw_appuser u
        left join hr_person p on p.id = u.person_id
        left join hr_salary_employee_level m on m.id_fwuser = u.id
        left join hr_salary_base_level b on b.id = m.id_salary
        where (
            :keyword is null
            or lower(u.username) like lower(concat('%', :keyword, '%'))
            or lower(u.email) like lower(concat('%', :keyword, '%'))
            or lower(coalesce(p.first_name, '')) like lower(concat('%', :keyword, '%'))
            or lower(coalesce(p.last_name,  '')) like lower(concat('%', :keyword, '%'))
        )
        order by
            case
              when btrim(coalesce(p.first_name,'') || ' ' || coalesce(p.last_name,'')) = '' then u.username
              else btrim(coalesce(p.first_name,'') || ' ' || coalesce(p.last_name,''))
            end,
            coalesce(p.last_name,'')
        """,
            nativeQuery = true)
    List<UserLevelProjection> findUserLevelRows(@Param("keyword") String keyword);
}
