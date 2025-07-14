package com.fusi24.pangreksa.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Stream;

@Slf4j
@Component
public class SqlRunner implements ApplicationRunner {

    private final DataSource dataSource;

    public SqlRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Stream<java.nio.file.Path> paths = Files.list(Paths.get("src/main/resources"))) {
            paths.filter(path -> path.toString().endsWith(".sql"))
                 .sorted((p1, p2) -> p1.getFileName().toString().compareTo(p2.getFileName().toString()))
                 .forEach(path -> {
                     try {
                         String sql = new String(Files.readAllBytes(path));
                         try (Connection conn = dataSource.getConnection();
                              Statement stmt = conn.createStatement()) {
                             stmt.execute(sql);
                         }
                         log.info("✅ Executed SQL script: " + path.getFileName());
                     } catch (IOException | java.sql.SQLException e) {
                         log.info("❌ Failed to execute SQL script: " + path.getFileName());
                         e.printStackTrace();
                     }
                 });
        }
        log.info("✅ Manual SQL script executed.");
    }
}
