package com.fusi24.pangreksa.web;

import com.fusi24.pangreksa.web.model.Responsibility;
import com.fusi24.pangreksa.web.model.entity.FwAppUser;
import com.fusi24.pangreksa.web.model.entity.VwAppUserAuth;
import com.fusi24.pangreksa.web.repo.FwAppUserRepository;
import com.fusi24.pangreksa.web.repo.VwAppUserAuthRepository;
import com.fusi24.pangreksa.web.service.AppUserAuthService;
import com.vaadin.flow.server.menu.MenuEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

//@DataJpaTest
@Slf4j
@SpringBootTest
public class VwAppUserAuthTest {

    @Autowired
    private FwAppUserRepository appUserRepository;
    @Autowired
    private VwAppUserAuthRepository appUserAuthRepository;
    @Autowired
    private AppUserAuthService appUserAuthService;

    @PersistenceContext
    private EntityManager entityManager;

    void loadSqlScripts(Path path) throws Exception {
        String sql = Files.readString(path);
        for (String statement : sql.split(";")) {
            if (!statement.trim().isEmpty()) {
                entityManager.createNativeQuery(statement).executeUpdate();
            }
        }
        log.info("✅ Test data loaded.");
    }

//    @BeforeEach               // Open this if Test is @DataJpaTest
//    @Transactional            // Open this if Test is @DataJpaTest
    void loadTestData() throws Exception {
        try (Stream<Path> paths = Files.list(Paths.get("src/main/resources"))) {
            paths.filter(path -> path.toString().endsWith(".sql"))
                    .sorted((p1, p2) -> p1.getFileName().toString().compareTo(p2.getFileName().toString()))
                    .forEach(path -> {
                        try {
                            loadSqlScripts(path);
                            log.info("✅ Executed SQL script: " + path.getFileName());
                        } catch (IOException | java.sql.SQLException e) {
                            log.info("❌ Failed to execute SQL script: " + path.getFileName());
                            e.printStackTrace();
                        } catch (Exception e) {
                            log.info("❌ Error not found: " + path.getFileName());
                        }
                    });
        }
        log.info("✅ Manual SQL script executed.");
    }

    @Test
    void shouldQueryAllAppUsersAuth() {
        List<FwAppUser> users = appUserRepository.findAll();
        assertThat(users).isNotNull();

        for (FwAppUser user : users) {
            List<VwAppUserAuth> appUserAuthList = appUserAuthRepository.findAllByIsActiveTrueAndUsernameOrderByResponsibilityAsc(user.getUsername());
            assertThat(appUserAuthList).isNotNull();

            log.info("size of Auth User List : " + appUserAuthList.size());

            appUserAuthList.forEach(auth -> {
                log.info("ID: " + auth.getId() + ", Username: " + auth.getUsername() + ", Email: " + auth.getEmail() +
                        ", Responsibility: " + auth.getResponsibility() + ", Label: " + auth.getLabel() +
                        ", URL: " + auth.getUrl() + ", Sort Order: " + auth.getSortOrder() +
                        ", Page Icon: " + auth.getPageIcon());
            });

            log.info("User: " + user.getUsername() + ", Email: " + user.getEmail());

            // Get Distinct Responsibilities
            Set<String> distinctResponsibilities = appUserAuthList.stream()
                    .map(VwAppUserAuth::getResponsibility)
                    .collect(Collectors.toSet());

            log.info("Distinct Responsibilities: " + distinctResponsibilities.size());

            distinctResponsibilities.forEach(r -> {
                System.out.println(r);

                Responsibility responsibility = new Responsibility(r);

                appUserAuthList.stream()
                        .filter(auth -> auth.getResponsibility().equals(r))
                        .forEach(auth -> {
                            log.info("ID: " + auth.getId() +", Menu: " + auth.getLabel() + ", URL: " + auth.getUrl() + ", Responsibility: " + auth.getResponsibility());
                            responsibility.addMenu(new MenuEntry(
                                    auth.getUrl(),
                                    auth.getLabel(),
                                    auth.getSortOrder(),
                                    auth.getPageIcon(), null
                            ));
                        });

                log.info("Responsibility: " + responsibility.getResponsibility() + ", Menus: " + responsibility.getMenuEntries().size());
            });


//            for (VwAppUserAuth appUserAuth : appUserAuthList) {
//                log.info("  Responsibility: " + appUserAuth.getResponsibility() + ", Pages: " + appUserAuth.getLabel() + ", url: " + appUserAuth.getUrl());
//            }
        }


    }

    @Test
    void serviceShouldreturnResponsibilities() {
        List<FwAppUser> users = appUserRepository.findAll();
        assertThat(users).isNotNull();

        for (FwAppUser user : users) {
            log.info("Username: " + user.getUsername());
            List<Responsibility> responsibilities = appUserAuthService.getAllResponsibilitiesFromUsername(user.getUsername());
            assertThat(responsibilities).isNotNull();

            responsibilities.forEach(r -> {
                log.info("Responsibility: " + r.getResponsibility() + ", Menus: " + r.getMenuEntries().size());
                r.getMenuEntries().forEach(m -> log.info("  Menu: " + m.title() + ", URL: " + m.path()));
            });
        }
    }

}
