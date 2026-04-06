package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.web.model.entity.Campaign;
import com.fusi24.pangreksa.web.repo.CampaignRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CampaignService {

    private final CampaignRepository repository;

    @Autowired
    public CampaignService(CampaignRepository repository) {
        this.repository = repository;
    }

    // Untuk Dashboard Karyawan
    public List<Campaign> getActiveCampaigns() {
        return repository.findActiveCampaignsForDashboard();
    }

    // Untuk Table Manajemen Admin/HR
    public List<Campaign> getAllCampaigns() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Campaign save(Campaign campaign, Long currentUserId) {
        if (campaign.getId() == null) {
            // Jika data baru, set siapa pembuatnya
            campaign.setCreatedBy(currentUserId);
        }
        // Setiap ada perubahan, update siapa yang terakhir mengubah
        campaign.setUpdatedBy(currentUserId);

        return repository.save(campaign);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Optional<Campaign> findById(Long id) {
        return repository.findById(id);
    }
}