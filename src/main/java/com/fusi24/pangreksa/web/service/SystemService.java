package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.web.model.entity.FwSystem;
import com.fusi24.pangreksa.web.repo.FwSystemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Service
public class SystemService {
    private static final Logger log = LoggerFactory.getLogger(SystemService.class);

    // Variabel repository sesuai dengan constructor Anda
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
        List<UUID> ids = idStrings.stream().map(UUID::fromString).toList();
        return systemRepository.findByIdInOrderBySortOrderAsc(ids);
    }

    public FwSystem findSystemById(UUID id) {
        log.debug("Fetching system with ID: {}", id);
        return systemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("System not found with ID: " + id));
    }

    /**
     * Mencari data FwSystem berdasarkan key.
     * Karena findByKey mengembalikan List, kita ambil yang pertama saja.
     */
    public FwSystem findSystemByKey(String configKey) {
        log.debug("Fetching system with Key: {}", configKey);
        return systemRepository.findByKey(configKey)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("System not found with Key: " + configKey));
    }

    public FwSystem saveSystem(FwSystem system) {
        log.debug("Saving system with key: {}", system.getKey());
        return systemRepository.save(system);
    }

    // --- HELPER UNTUK MENGAMBIL NILAI CONFIG ---

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

    /**
     * Method ini sudah diperbaiki untuk menangani List hasil findByKey
     * dan menggunakan variabel systemRepository yang benar.
     */
    public BigDecimal getDecimalConfig(String key) {
        return systemRepository.findByKey(key).stream()
                .findFirst()
                .map(sys -> {
                    // Cek jika field decimal_val memiliki data
                    if (sys.getDecimalVal() != null) return sys.getDecimalVal();

                    // Jika null, coba parsing dari string_val (menghapus koma jika ada)
                    try {
                        String rawValue = sys.getStringVal();
                        if (rawValue != null && !rawValue.isEmpty()) {
                            return new BigDecimal(rawValue.replace(",", ""));
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse config string_val to BigDecimal for key: {}", key);
                    }
                    return BigDecimal.ZERO;
                })
                .orElse(BigDecimal.ZERO);
    }

    // Method tambahan jika Anda membutuhkan nilai String secara langsung
    public String getConfigStringValue(String configKey) {
        try {
            FwSystem config = findSystemByKey(configKey);
            return config.getStringVal();
        } catch (Exception e) {
            return "";
        }
    }
}