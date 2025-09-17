package com.fusi24.pangreksa.web.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.fusi24.pangreksa.web.model.entity.HrSalaryEmployeeLevel;

import java.util.List;
import java.util.Optional;

public interface HrSalaryEmployeeLevelRepository extends JpaRepository<HrSalaryEmployeeLevel, Long> {

    Optional<HrSalaryEmployeeLevel> findByAppUser_Id(Long appUserId);

    @Query(value = """
        select u.id as user_id,
               u.username,
               u.email,
               u.nickname,
               p.first_name,
               p.last_name,
               u.company_id,
               m.id_hsel,
               m.id_hsbl,
               b.level_code,
               b.base_salary
        from fw_appuser u
        left join hr_person p on p.id = u.person_id
        left join hr_salary_employee_level m on m.id_fu = u.id
        left join hr_salary_base_level b on b.id = m.id_hsbl
        where (
            :keyword is null
            or lower(u.username) like lower(concat('%', :keyword, '%'))
            or lower(u.email) like lower(concat('%', :keyword, '%'))
            or lower(coalesce(p.first_name, '')) like lower(concat('%', :keyword, '%'))
            or lower(coalesce(p.last_name,  '')) like lower(concat('%', :keyword, '%'))
        )
        order by coalesce(p.first_name, u.username), coalesce(p.last_name, '')
        """,
            nativeQuery = true)
    List<Object[]> findUserLevelRows(@Param("keyword") String keyword);
}
