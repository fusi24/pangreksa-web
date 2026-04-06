package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.web.model.entity.Campaign;
import com.fusi24.pangreksa.web.repo.CampaignRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class CampaignService {

    private final CampaignRepository repository;
    private final String UPLOAD_DIR = "uploads/campaigns/";

    @Autowired
    public CampaignService(CampaignRepository repository) {
        this.repository = repository;
    }

    // Method untuk menyimpan gambar fisik dan mengembalikan path-nya
    public String saveImage(byte[] imageBytes) throws IOException {
        if (imageBytes == null) return null;

        Path directoryPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
        }

        String fileName = "banner_" + UUID.randomUUID().toString().substring(0, 8) + ".png";
        Path filePath = directoryPath.resolve(fileName);
        Files.write(filePath, imageBytes);

        return "/" + UPLOAD_DIR + fileName; // Mengembalikan path untuk web
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
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }

        try {
            // Hilangkan garis miring di awal jika ada agar terbaca sebagai relative path dari root project
            String cleanPath = imagePath.startsWith("/") ? imagePath.substring(1) : imagePath;
            Path path = Paths.get(cleanPath);

            if (Files.exists(path)) {
                return Files.readAllBytes(path);
            } else {
                // Log jika file tidak ditemukan untuk memudahkan debugging
                System.err.println("File tidak ditemukan di path: " + path.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Gagal membaca file gambar: " + e.getMessage());
        }
        return null;
    }
}