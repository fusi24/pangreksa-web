package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.web.model.entity.Campaign;
import com.fusi24.pangreksa.web.repo.CampaignRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class CampaignService {

    private static final Logger log = LoggerFactory.getLogger(CampaignService.class);
    private final CampaignRepository repository;
    private final String UPLOAD_DIR = "uploads/campaigns/";

    @Autowired
    public CampaignService(CampaignRepository repository) {
        this.repository = repository;
    }

    public String saveImage(byte[] imageBytes) throws IOException {
        if (imageBytes == null) return null;

        Path directoryPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
        }

        String fileName = "banner_" + UUID.randomUUID().toString().substring(0, 8) + ".png";
        Path filePath = directoryPath.resolve(fileName);
        Files.write(filePath, imageBytes);

        return "/" + UPLOAD_DIR + fileName;
    }

    @Transactional
    public Campaign save(Campaign campaign, Long currentUserId) {
        if (campaign.getId() == null) {
            campaign.setCreatedBy(currentUserId);
        }
        campaign.setUpdatedBy(currentUserId);
        return repository.save(campaign);
    }

    public List<Campaign> getAllCampaigns() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public List<Campaign> getActiveCampaigns() {
        return repository.findActiveCampaignsForDashboard();
    }

    public byte[] getImagePathAsByteArray(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return null;

        try {
            String cleanPath = imagePath.startsWith("/") ? imagePath.substring(1) : imagePath;
            Path path = Paths.get(cleanPath);
            if (Files.exists(path)) {
                return Files.readAllBytes(path);
            }
        } catch (IOException e) {
            log.error("Gagal membaca file gambar: {}", e.getMessage());
        }
        return null;
    }

    public String calculateStatus(Campaign c) {
        if (!c.isActive()) return "DRAFT";

        LocalDate now = LocalDate.now();
        if (now.isBefore(c.getStartDate())) return "TERJADWAL";
        if (now.isAfter(c.getEndDate())) return "BERAKHIR";
        return "AKTIF";
    }

    @Transactional
    public void incrementViewCount(Long campaignId) {
        // PERBAIKAN: Gunakan parameter campaignId
        repository.findById(campaignId).ifPresent(c -> {
            c.setViewCount(c.getViewCount() + 1);
            repository.save(c);
        });
    }

    @Transactional
    public void incrementClickCount(Long campaignId) {
        // PERBAIKAN: Gunakan parameter campaignId
        repository.findById(campaignId).ifPresent(c -> {
            c.setClickCount(c.getClickCount() + 1);
            repository.save(c);
        });
    }

    @Transactional
    public void deleteCampaign(Campaign campaign) {
        if (campaign.getImagePath() != null) {
            try {
                String cleanPath = campaign.getImagePath().startsWith("/") ?
                        campaign.getImagePath().substring(1) : campaign.getImagePath();
                Files.deleteIfExists(Paths.get(cleanPath));
            } catch (IOException e) {
                log.error("Gagal menghapus file gambar: {}", e.getMessage());
            }
        }
        repository.delete(campaign);
    }
}