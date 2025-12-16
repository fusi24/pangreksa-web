package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.web.model.entity.MasterPtkp;
import com.fusi24.pangreksa.web.repo.MasterPtkpRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MasterPtkpService {

    private final MasterPtkpRepository repo;

    public MasterPtkpService(MasterPtkpRepository repo) {
        this.repo = repo;
    }

    public List<MasterPtkp> findAll() {
        return repo.findAll();
    }

    public MasterPtkp save(MasterPtkp ptkp) {
        return repo.save(ptkp);
    }

    public void delete(MasterPtkp ptkp) {
        repo.delete(ptkp);
    }
}
