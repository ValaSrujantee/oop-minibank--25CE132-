package service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class ReportGenerator {
    public String generate(Path logsDirectory, Path reportFile) throws IOException {
        Files.createDirectories(logsDirectory);
        long deposits = 0;
        long withdrawals = 0;
        List<String> fileDetails = new ArrayList<>();

        try (DirectoryStream<Path> logs = Files.newDirectoryStream(logsDirectory, "*.log")) {
            for (Path log : logs) {
                BasicFileAttributes attributes = Files.readAttributes(log, BasicFileAttributes.class);
                fileDetails.add(log.getFileName() + " | size=" + attributes.size() + " bytes | modified=" +
                        Instant.ofEpochMilli(attributes.lastModifiedTime().toMillis()));
                try (BufferedReader reader = Files.newBufferedReader(log, StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] fields = line.split("\\|", -1);
                        if (fields.length < 4) continue;
                        try {
                            long amount = Long.parseLong(fields[3]);
                            switch (fields[1]) {
                                case "DEPOSIT", "TRANSFER_IN" -> deposits = Math.addExact(deposits, amount);
                                case "WITHDRAW", "TRANSFER_OUT" -> withdrawals = Math.addExact(withdrawals, amount);
                                default -> { }
                            }
                        } catch (NumberFormatException ignored) {
                            // Ignore malformed rows so one bad line cannot invalidate the report.
                        }
                    }
                }
            }
        }

        StringBuilder report = new StringBuilder("MINIBANK END-OF-DAY REPORT\n")
                .append("Total deposits (including transfers in): ").append(deposits).append('\n')
                .append("Total withdrawals (including transfers out): ").append(withdrawals).append('\n')
                .append("Log files:\n");
        if (fileDetails.isEmpty()) report.append("(none)\n");
        else fileDetails.forEach(detail -> report.append(detail).append('\n'));

        Path parent = reportFile.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(reportFile, report, StandardCharsets.UTF_8);
        return report.toString();
    }
}
