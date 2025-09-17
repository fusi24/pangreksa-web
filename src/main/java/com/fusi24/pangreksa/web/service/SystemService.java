package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.web.model.entity.FwSystem;
import com.fusi24.pangreksa.web.repo.FwSystemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    public List<FwSystem> findSystemsByIds(List<String> idStrings) {
        log.debug("Fetching systems ordered by idStrings: {}", idStrings.size());

        // create list of UUIDs from list of strings
        List<UUID> ids = idStrings.stream().map(UUID::fromString).toList();

        return systemRepository.findByIdInOrderBySortOrderAsc(ids);
    }

    public FwSystem findSystemById(UUID id) {
        log.debug("Fetching system with ID: {}", id);
        return systemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("System not found with ID: " + id));
    }

    public FwSystem findSystemByKey(String configKey) {
        log.debug("Fetching system with Key: {}", configKey);
        return systemRepository.findByKey(configKey)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("System not found with Key: " + configKey));
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

    public int getMaxSearchResult(){
        return findSystemById(UUID.fromString("f56c31fb-f1c0-4660-adc5-1a458fccb969")).getIntVal();
    }

    public int getMinSearchLength(){
        return findSystemById(UUID.fromString("f56c31fb-f1c0-4660-adc5-1a458fccb970")).getIntVal();
    }

    public BigDecimal getConfigNumericValue(String configKey) {
        FwSystem config = findSystemByKey(configKey); // Assuming findSystemById(key) returns FwSystem by 'key'
        if (config == null) {
            throw new RuntimeException("Config not found: " + configKey);
        }

        if (config.getIntVal() != null) {
            return BigDecimal.valueOf(config.getIntVal());
        } else if (config.getStringVal() != null) {
            // Handle formatted numbers like "12,000,000" → remove commas
            String clean = config.getStringVal().replace(",", "");
            return new BigDecimal(clean);
        } else {
            throw new RuntimeException("Config value not available or invalid for key: " + configKey);
        }
    }

    public String getConfigStringValue(String configKey) {
        FwSystem config = findSystemByKey(configKey);
        if (config == null || config.getStringVal() == null) {
            throw new RuntimeException("String config not found: " + configKey);
        }
        return config.getStringVal();
    }
}
