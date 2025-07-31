package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.web.model.entity.FwSystem;
import com.fusi24.pangreksa.web.repo.FwSystemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SystemService {
    private static final Logger log = LoggerFactory.getLogger(SystemService.class);

    private final FwSystemRepository systemRepository;

    public SystemService(FwSystemRepository systemRepository) {
        this.systemRepository = systemRepository;
    }

    public List<FwSystem> findAllSystems() {
        log.debug("Fetching all systems ordered by sort order");
        return systemRepository.findAllOrderBySortOrderAsc();
    }

    public FwSystem findSystemById(UUID id) {
        log.debug("Fetching system with ID: {}", id);
        return systemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("System not found with ID: " + id));
    }

    public FwSystem saveSystem(FwSystem system) {
        log.debug("Saving system with key: {}", system.getKey());
        return systemRepository.save(system);
    }

    public String getStringAppName() {
        return findSystemById(UUID.fromString("2af9be5b-426e-499e-b03a-57849e5217c4")).getStringVal();
    }

    public String getStringAppLogo() {
        return findSystemById(UUID.fromString("cf2a0798-a1f9-4235-baca-b70e5b6788bc")).getStringVal();
    }

    public String getStringDataPath() {
        return findSystemById(UUID.fromString("a4b91eca-9367-4b90-8ac2-71115817056f")).getStringVal();
    }
}
