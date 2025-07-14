package com.fusi24.pangreksa.web;

import com.fusi24.pangreksa.web.model.entity.FwAppUser;
import com.fusi24.pangreksa.web.repo.FwAppUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Slf4j
public class AppUserTest {

    @Autowired
    private FwAppUserRepository appUserRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    @Transactional
    void loadSqlScripts() throws Exception {
        String sql = Files.readString(Paths.get("src/main/resources/01_fw_appuser_202507112244.sql"));
        for (String statement : sql.split(";")) {
            if (!statement.trim().isEmpty()) {
                entityManager.createNativeQuery(statement).executeUpdate();
            }
        }
        log.info("✅ Test data loaded.");
    }

    @Test
    void shouldQueryAllAppUsers() {
        List<FwAppUser> users = appUserRepository.findAll();
        log.info("Total users found: " + users.size());

        assertThat(users).isNotNull();
    }
}
