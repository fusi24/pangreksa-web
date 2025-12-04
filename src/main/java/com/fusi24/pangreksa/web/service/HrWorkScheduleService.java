package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.web.model.entity.FwAppUser;
import com.fusi24.pangreksa.web.model.entity.HrPersonPosition;
import com.fusi24.pangreksa.web.model.entity.HrWorkSchedule;
import com.fusi24.pangreksa.web.model.entity.HrWorkScheduleAssignment;
import com.fusi24.pangreksa.web.repo.HrPersonPositionRepository;
import com.fusi24.pangreksa.web.repo.HrWorkScheduleAssignmentRepository;
import com.fusi24.pangreksa.web.repo.HrWorkScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class HrWorkScheduleService {

    private final HrPersonPositionRepository personPositionRepo;
    private final HrWorkScheduleAssignmentRepository scheduleAssignmentRepo;
    private final HrWorkScheduleRepository workScheduleRepo;

    @Autowired
    public HrWorkScheduleService(
            HrPersonPositionRepository personPositionRepo,
            HrWorkScheduleAssignmentRepository scheduleAssignmentRepo,
            HrWorkScheduleRepository workScheduleRepo) {
        this.personPositionRepo = personPositionRepo;
        this.scheduleAssignmentRepo = scheduleAssignmentRepo;
        this.workScheduleRepo = workScheduleRepo;
    }

    /**
     * Gets the active work schedule for a user on a given date.
     * Flow:
     * 1. Find user's current position (via HrPerson → HrPersonPosition)
     * 2. Get org_structure_id from that position
     * 3. Find the latest work schedule assignment for that org structure (effective <= date)
     * 4. Fetch the actual HrWorkSchedule
     */
    public HrWorkSchedule getActiveScheduleForUser(FwAppUser user, LocalDate date) {
        if (user == null || user.getPerson() == null) {
            return null;
        }

        Long personId = user.getPerson().getId();

        // 1. Get current active position
        HrPersonPosition position = personPositionRepo.findFirstByPersonId(personId);

        if (position == null) {
            return null;
        }

        HrPersonPosition currentPos = position;
        Long orgStructureId = position.getPosition().getOrgStructure().getId();

        // 2. Find schedule assignment for this org structure
        Optional<HrWorkScheduleAssignment> assignmentOpt = scheduleAssignmentRepo.findByOrgStructureId(
                        orgStructureId
                );

        HrWorkSchedule scheduleAll = workScheduleRepo.findFirstByAssignmentScope("All").orElse(null);

        if (assignmentOpt.isEmpty() && scheduleAll == null) {
            return null;
        }

        Long scheduleId = assignmentOpt.map(p -> p.getSchedule()).map(HrWorkSchedule::getId).orElse(scheduleAll.getId());

        // 3. Fetch the actual schedule
        return workScheduleRepo.findById(scheduleId).orElse(null);
    }

    public boolean hasActiveScheduleForUser(FwAppUser appUser, LocalDate date) {
        return getActiveScheduleForUser(appUser, date) != null;
    }
}