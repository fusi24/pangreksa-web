package com.fusi24.pangreksa.web.service;

import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.web.model.entity.FwAppUser;
import com.fusi24.pangreksa.web.model.entity.HrAttendance;
import com.fusi24.pangreksa.web.model.entity.HrCompanyBranch;
import com.fusi24.pangreksa.web.model.entity.HrWorkSchedule;
import com.fusi24.pangreksa.web.repo.FwAppUserRepository;
import com.fusi24.pangreksa.web.repo.HrAttendanceRepository;
import com.fusi24.pangreksa.web.repo.HrCompanyBranchRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.Getter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.DateUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AttendanceImportService {

    private static final ZoneId JAKARTA_ZONE = ZoneId.of("Asia/Jakarta");

    private final HrAttendanceRepository attendanceRepo;
    private final FwAppUserRepository appUserRepository;
    private final HrCompanyBranchRepository branchRepository;
    private final HrWorkScheduleService workScheduleService;
    private final AttendanceService attendanceService;

    @PersistenceContext
    private EntityManager em;

    public AttendanceImportService(HrAttendanceRepository attendanceRepo,
                                   FwAppUserRepository appUserRepository,
                                   HrCompanyBranchRepository branchRepository,
                                   HrWorkScheduleService workScheduleService,
                                   AttendanceService attendanceService) {
        this.attendanceRepo = attendanceRepo;
        this.appUserRepository = appUserRepository;
        this.branchRepository = branchRepository;
        this.workScheduleService = workScheduleService;
        this.attendanceService = attendanceService;
    }

    @Getter
    public static class ImportResult {
        private int inserted;
        private int updated;
        private int skippedNoUser;
        private int skippedNoSchedule;
        private int skippedInvalidRow;
        private final List<String> errors = new ArrayList<>();

        public int totalProcessed() {
            return inserted + updated + skippedNoUser + skippedNoSchedule + skippedInvalidRow;
        }
    }

    @Transactional
    public ImportResult importAttendance(Path filePath,
                                         String filename,
                                         String notes,
                                         Long branchId,
                                         AppUserInfo modifier) {

        try (InputStream in = Files.newInputStream(filePath)) {
            return importAttendance(in, filename, notes, branchId, modifier);
        } catch (IOException e) {
            ImportResult r = new ImportResult();
            r.getErrors().add("Gagal membaca file: " + e.getMessage());
            return r;
        }
    }

    // ✅ ini yang dipakai UI kamu kalau sudah pegang InputStream langsung
    @Transactional
    public ImportResult importAttendance(InputStream fileStream,
                                         String filename,
                                         String notes,
                                         Long branchId,
                                         AppUserInfo modifier) {

        ImportResult result = new ImportResult();

        HrCompanyBranch branch = null;
        if (branchId != null) {
            branch = branchRepository.findById(branchId).orElse(null);
        }

        List<RowData> rows;
        try {
            rows = parseFile(fileStream, filename);
        } catch (Exception ex) {
            result.getErrors().add("Gagal parse file: " + ex.getMessage());
            return result;
        }

        for (int i = 0; i < rows.size(); i++) {
            RowData r = rows.get(i);

            try {
                if (r.noId == null || r.attendanceDate == null) {
                    result.skippedInvalidRow++;
                    continue;
                }

                // match fw_appuser.id
                FwAppUser user = appUserRepository.findById(r.noId).orElse(null);
                if (user == null) {
                    result.skippedNoUser++;
                    continue;
                }

                HrWorkSchedule schedule = workScheduleService.getActiveScheduleForUser(user, r.attendanceDate);
                if (schedule == null) {
                    result.skippedNoSchedule++;
                    continue;
                }

                HrAttendance att = attendanceRepo
                        .findByAppUserIdAndAttendanceDate(user.getId(), r.attendanceDate)
                        .orElseGet(HrAttendance::new);

                boolean isNew = (att.getId() == null);

                att.setAppUser(user);
                att.setPerson(user.getPerson());
                att.setWorkSchedule(schedule);
                att.setAttendanceDate(r.attendanceDate);

                att.setCheckIn(r.checkIn);
                att.setCheckOut(r.checkOut);

                // notes dari dialog (dipakai untuk semua row)
                if (notes != null && !notes.trim().isEmpty()) {
                    att.setNotes(notes.trim());
                }

                // branch copy ke kolom attendance
                if (branch != null) {
                    att.setBranchCode(branch.getBranchCode());
                    att.setBranchName(branch.getBranchName());
                    att.setBranchAddress(branch.getBranchAddress());
                }

                // status + total work minutes dll ikut logic existing
                attendanceService.saveAttendance(att, modifier);

                if (isNew) result.inserted++;
                else result.updated++;

            } catch (Exception ex) {
                result.getErrors().add("Row #" + (i + 2) + " error: " + ex.getMessage()); // +2 header
                result.skippedInvalidRow++;

                if (result.getErrors().size() > 20) {
                    result.getErrors().add("Error terlalu banyak, sisanya dipotong.");
                    break;
                }
            }
        }

        try {
            callProcessUploadedAttendancePenalty();
        } catch (Exception ex) {
            result.getErrors().add("SP penalty gagal dijalankan: " + ex.getMessage());
        }

        return result;
    }

    private void callProcessUploadedAttendancePenalty() {
        // Postgres: CALL sp_process_uploaded_attendance_penalty();
        em.createNativeQuery("CALL sp_process_uploaded_attendance_penalty()").executeUpdate();
    }

    // =========================
    // Parsing
    // =========================
    private static class RowData {
        Long noId;
        LocalDate attendanceDate;
        LocalDateTime checkIn;
        LocalDateTime checkOut;
    }

    private List<RowData> parseFile(InputStream is, String filename) throws Exception {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);

        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) return parseExcel(is);
        if (lower.endsWith(".csv")) return parseCsv(is);

        throw new IllegalArgumentException("Format file tidak didukung: " + filename);
    }

    private List<RowData> parseExcel(InputStream is) throws Exception {
        List<RowData> out = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) return out;

            DataFormatter fmt = new DataFormatter(Locale.US);
            Map<String, Integer> headerMap = readHeaderMap(sheet.getRow(0), fmt);

            Integer cNoId = headerMap.get("no. id");
            Integer cTanggal = headerMap.get("tanggal");
            Integer cScanMasuk = headerMap.get("scan masuk");
            Integer cScanPulang = headerMap.get("scan pulang");

            if (cNoId == null || cTanggal == null) {
                throw new IllegalStateException("Header wajib tidak ditemukan: 'No. ID' dan/atau 'Tanggal'");
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String noIdStr = fmt.formatCellValue(row.getCell(cNoId)).trim();
                if (noIdStr.isEmpty()) continue;

                Long noId = parseLongSafe(noIdStr);
                LocalDate date = parseDateCell(row.getCell(cTanggal), fmt);
                if (noId == null || date == null) continue;

                String inStr = cScanMasuk == null ? "" : fmt.formatCellValue(row.getCell(cScanMasuk)).trim();
                String outStr = cScanPulang == null ? "" : fmt.formatCellValue(row.getCell(cScanPulang)).trim();

                RowData rd = new RowData();
                rd.noId = noId;
                rd.attendanceDate = date;
                rd.checkIn = combineDateTime(date, inStr);
                rd.checkOut = combineDateTime(date, outStr);

                out.add(rd);
            }
        }

        return out;
    }

    private Map<String, Integer> readHeaderMap(Row header, DataFormatter fmt) {
        Map<String, Integer> map = new HashMap<>();
        if (header == null) return map;

        for (Cell cell : header) {
            String key = fmt.formatCellValue(cell).trim().toLowerCase(Locale.ROOT);
            if (!key.isEmpty()) map.put(key, cell.getColumnIndex());
        }
        return map;
    }

    private LocalDate parseDateCell(Cell cell, DataFormatter fmt) {
        if (cell == null) return null;

        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                Date d = cell.getDateCellValue();
                return d.toInstant().atZone(JAKARTA_ZONE).toLocalDate();
            }

            String s = fmt.formatCellValue(cell).trim();
            if (s.isEmpty()) return null;

            List<DateTimeFormatter> patterns = List.of(
                    DateTimeFormatter.ofPattern("M/d/yyyy"),
                    DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                    DateTimeFormatter.ofPattern("d/M/yyyy"),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy")
            );

            for (DateTimeFormatter p : patterns) {
                try { return LocalDate.parse(s, p); } catch (Exception ignored) {}
            }

            return LocalDate.parse(s);
        } catch (Exception ex) {
            return null;
        }
    }

    private List<RowData> parseCsv(InputStream is) throws Exception {
        List<RowData> out = new ArrayList<>();

        List<String> lines;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            lines = br.lines().toList();
        }
        if (lines.isEmpty()) return out;

        String[] headers = splitCsvLine(lines.get(0));
        Map<String, Integer> headerMap = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            headerMap.put(headers[i].trim().toLowerCase(Locale.ROOT), i);
        }

        Integer cNoId = headerMap.get("no. id");
        Integer cTanggal = headerMap.get("tanggal");
        Integer cScanMasuk = headerMap.get("scan masuk");
        Integer cScanPulang = headerMap.get("scan pulang");

        if (cNoId == null || cTanggal == null) {
            throw new IllegalStateException("Header wajib tidak ditemukan: 'No. ID' dan/atau 'Tanggal'");
        }

        for (int r = 1; r < lines.size(); r++) {
            String line = lines.get(r);
            if (line == null || line.trim().isEmpty()) continue;

            String[] cols = splitCsvLine(line);

            String noIdStr = getCsv(cols, cNoId);
            String dateStr = getCsv(cols, cTanggal);
            if (noIdStr.isBlank() || dateStr.isBlank()) continue;

            Long noId = parseLongSafe(noIdStr);
            LocalDate date = parseDateString(dateStr);
            if (noId == null || date == null) continue;

            String inStr = cScanMasuk == null ? "" : getCsv(cols, cScanMasuk);
            String outStr = cScanPulang == null ? "" : getCsv(cols, cScanPulang);

            RowData rd = new RowData();
            rd.noId = noId;
            rd.attendanceDate = date;
            rd.checkIn = combineDateTime(date, inStr);
            rd.checkOut = combineDateTime(date, outStr);

            out.add(rd);
        }

        return out;
    }

    private String[] splitCsvLine(String line) {
        return line.split(",", -1);
    }

    private String getCsv(String[] cols, int idx) {
        if (cols == null || idx < 0 || idx >= cols.length) return "";
        return cols[idx] == null ? "" : cols[idx].trim();
    }

    private LocalDate parseDateString(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;

        List<DateTimeFormatter> patterns = List.of(
                DateTimeFormatter.ofPattern("M/d/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy")
        );

        for (DateTimeFormatter p : patterns) {
            try { return LocalDate.parse(s, p); } catch (Exception ignored) {}
        }

        try { return LocalDate.parse(s); } catch (Exception ex) { return null; }
    }

    private LocalDateTime combineDateTime(LocalDate date, String timeStr) {
        if (date == null) return null;
        if (timeStr == null) return null;

        String s = timeStr.trim();
        if (s.isEmpty()) return null;

        try {
            LocalTime t = LocalTime.parse(s, DateTimeFormatter.ofPattern("H:mm"));
            return LocalDateTime.of(date, t);
        } catch (Exception ignored) {}

        try {
            LocalTime t = LocalTime.parse(s, DateTimeFormatter.ofPattern("HH:mm"));
            return LocalDateTime.of(date, t);
        } catch (Exception ex) {
            return null;
        }
    }

    private Long parseLongSafe(String s) {
        try {
            String x = s.trim().replace(".0", "");
            return Long.parseLong(x);
        } catch (Exception ex) {
            return null;
        }
    }
}
