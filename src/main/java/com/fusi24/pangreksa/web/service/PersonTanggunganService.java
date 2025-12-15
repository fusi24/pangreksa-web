package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.web.model.entity.HrPerson;
import com.fusi24.pangreksa.web.model.entity.HrPersonTanggungan;
import com.fusi24.pangreksa.web.repo.HrPersonTanggunganRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PersonTanggunganService {

    @Transactional
    public void delete(HrPersonTanggungan t) {
        repository.delete(t);
    }

    public List<HrPersonTanggungan> findByPerson(HrPerson person) {
        return repository.findByPerson(person);
    }

    private final HrPersonTanggunganRepository repository;

    public PersonTanggunganService(HrPersonTanggunganRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public HrPersonTanggungan save(HrPersonTanggungan t) {
        return repository.save(t);
    }

}
