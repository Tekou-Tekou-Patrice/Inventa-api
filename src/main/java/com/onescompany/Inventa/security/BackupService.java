package com.onescompany.Inventa.security;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.sql.DataSource;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.onescompany.Inventa.domain.EvidenceAttachmentRepository;

@Service
public class BackupService {
    private static final String DATABASE_ENTRY = "database.bin";
    private static final String FORMAT_VERSION = "INVENTA_BACKUP_V1";
    private static final int MAX_TABLES = 500;
    private static final int MAX_COLUMNS = 500;
    private static final int MAX_VALUE_BYTES = 64 * 1024 * 1024;
    private static final long MAX_BACKUP_BYTES = 2L * 1024 * 1024 * 1024;

    private final DataSource dataSource;
    private final EvidenceAttachmentRepository evidenceAttachments;
    private final EvidenceStorageService evidenceStorage;

    public BackupService(
            DataSource dataSource,
            EvidenceAttachmentRepository evidenceAttachments,
            EvidenceStorageService evidenceStorage) {
        this.dataSource = dataSource;
        this.evidenceAttachments = evidenceAttachments;
        this.evidenceStorage = evidenceStorage;
    }

    public Path createBackup() throws IOException, SQLException {
        Path backup = Files.createTempFile("inventa-backup-", ".zip");
        try (Connection connection = dataSource.getConnection();
                ZipOutputStream zip = new ZipOutputStream(
                        new BufferedOutputStream(Files.newOutputStream(backup)))) {
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            connection.setAutoCommit(false);
            List<TableInfo> tables = tables(connection);
            zip.putNextEntry(new ZipEntry(DATABASE_ENTRY));
            DataOutputStream manifest = new DataOutputStream(zip);
            manifest.writeUTF(FORMAT_VERSION);
            manifest.writeUTF(connection.getMetaData().getDatabaseProductName());
            manifest.writeUTF(Instant.now().toString());
            manifest.writeInt(tables.size());
            for (TableInfo table : tables) writeTableHeader(manifest, table);
            manifest.flush();
            zip.closeEntry();
            for (TableInfo table : tables) {
                zip.putNextEntry(new ZipEntry("tables/" + table.name() + ".bin"));
                writeTableRows(new DataOutputStream(zip), connection, table);
                zip.closeEntry();

                zip.putNextEntry(new ZipEntry("donnees-csv/" + table.name() + ".csv"));
                writeTableCsv(new BufferedWriter(
                        new OutputStreamWriter(zip, StandardCharsets.UTF_8)), connection, table);
                zip.closeEntry();
            }
            for (String storageKey : evidenceKeys(connection)) {
                Path file = evidenceStorage.resolve(storageKey);
                if (!Files.isRegularFile(file)) {
                    throw new IOException("A registered evidence file is missing.");
                }
                zip.putNextEntry(new ZipEntry("evidence/" + storageKey));
                Files.copy(file, zip);
                zip.closeEntry();
            }
            connection.rollback();
        } catch (IOException | SQLException | RuntimeException error) {
            Files.deleteIfExists(backup);
            throw error;
        }
        return backup;
    }

    public void restore(MultipartFile upload) throws IOException, SQLException {
        if (upload.isEmpty() || upload.getSize() > MAX_BACKUP_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Backup is empty or exceeds the 2 GB limit.");
        }
        Path backup = Files.createTempFile("inventa-restore-", ".zip");
        Path stagedEvidence = Files.createTempDirectory("inventa-evidence-restore-");
        try {
            upload.transferTo(backup);
            try (ZipFile zip = new ZipFile(backup.toFile())) {
                validateAndStageEvidence(zip, stagedEvidence);
                var database = zip.getEntry(DATABASE_ENTRY);
                if (database == null || database.isDirectory()) {
                    throw invalidBackup();
                }
                installStagedEvidence(stagedEvidence);
                try (Connection connection = dataSource.getConnection();
                        DataInputStream input = new DataInputStream(
                                new BufferedInputStream(zip.getInputStream(database)))) {
                    restoreDatabase(connection, input, zip);
                }
            }
            synchronizeEvidenceFiles(stagedEvidence);
        } finally {
            Files.deleteIfExists(backup);
            deleteDirectoryContents(stagedEvidence);
            Files.deleteIfExists(stagedEvidence);
        }
    }

    private void writeTableHeader(DataOutputStream output, TableInfo table) throws IOException {
        output.writeUTF(table.name());
        output.writeInt(table.columns().size());
        for (ColumnInfo column : table.columns()) {
            output.writeUTF(column.name());
            output.writeInt(column.sqlType());
            output.writeUTF(column.typeName());
        }
    }

    private void writeTableRows(DataOutputStream output, Connection connection, TableInfo table)
            throws IOException, SQLException {
        String sql = "SELECT " + quotedColumns(connection, table.columns())
                + " FROM " + quote(connection, table.name());
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                output.writeBoolean(true);
                for (int index = 0; index < table.columns().size(); index++) {
                    Object value = rows.getObject(index + 1);
                    output.writeBoolean(value != null);
                    if (value != null) {
                        byte[] encoded = value instanceof byte[] bytes
                                ? Base64.getEncoder().encode(bytes)
                                : value.toString().getBytes(StandardCharsets.UTF_8);
                        if (encoded.length > MAX_VALUE_BYTES) {
                            throw new IOException("A database value is too large for a backup.");
                        }
                        output.writeInt(encoded.length);
                        output.write(encoded);
                    }
                }
            }
            output.writeBoolean(false);
        }
    }

    private void writeTableCsv(Writer output, Connection connection, TableInfo table)
            throws IOException, SQLException {
        output.write('\uFEFF');
        for (int index = 0; index < table.columns().size(); index++) {
            if (index > 0) output.write(',');
            writeCsvCell(output, table.columns().get(index).name());
        }
        output.write("\r\n");

        String sql = "SELECT " + quotedColumns(connection, table.columns())
                + " FROM " + quote(connection, table.name());
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                for (int index = 0; index < table.columns().size(); index++) {
                    if (index > 0) output.write(',');
                    Object value = rows.getObject(index + 1);
                    String text = value == null
                            ? ""
                            : value instanceof byte[] bytes
                                    ? Base64.getEncoder().encodeToString(bytes)
                                    : value.toString();
                    writeCsvCell(output, text);
                }
                output.write("\r\n");
            }
        }
        output.flush();
    }

    private void writeCsvCell(Writer output, String value) throws IOException {
        int firstNonWhitespace = 0;
        while (firstNonWhitespace < value.length()
                && Character.isWhitespace(value.charAt(firstNonWhitespace))) {
            firstNonWhitespace++;
        }
        boolean prefixTextMarker = firstNonWhitespace < value.length()
                && "=+-@\t\r".indexOf(value.charAt(firstNonWhitespace)) >= 0;
        output.write('"');
        if (prefixTextMarker) output.write('\'');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '"') output.write("\"\"");
            else output.write(character);
        }
        output.write('"');
    }

    private void restoreDatabase(Connection connection, DataInputStream input, ZipFile zip)
            throws IOException, SQLException {
        boolean constraintsDisabled = false;
        boolean committed = false;
        try {
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            String format = input.readUTF();
            String sourceProduct = input.readUTF();
            input.readUTF();
            if (!FORMAT_VERSION.equals(format)) throw invalidBackup();
            String targetProduct = connection.getMetaData().getDatabaseProductName();
            if (!compatibleProducts(sourceProduct, targetProduct)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "This backup was created for an incompatible database engine.");
            }
            int tableCount = input.readInt();
            if (tableCount < 1 || tableCount > MAX_TABLES) throw invalidBackup();
            List<TableInfo> currentTables = tables(connection);
            if (currentTables.size() != tableCount) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "The backup schema does not match this application version.");
            }
            List<RestoredTable> restoredTables = new ArrayList<>();
            Set<String> seenTables = new HashSet<>();
            for (int index = 0; index < tableCount; index++) {
                RestoredTable table = readTableHeader(input);
                TableInfo actual = currentTables.stream()
                        .filter(candidate -> candidate.name().equals(table.name()))
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.CONFLICT, "The backup schema does not match this application version."));
                if (!seenTables.add(table.name()) || !sameColumns(actual, table.columns())) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT, "The backup schema does not match this application version.");
                }
                restoredTables.add(table);
            }
            if (input.read() != -1) throw invalidBackup();
            validateTableEntries(zip, restoredTables);
            disableConstraints(connection);
            constraintsDisabled = true;
            for (TableInfo table : currentTables) {
                try (var statement = connection.createStatement()) {
                    statement.executeUpdate("DELETE FROM " + quote(connection, table.name()));
                }
            }
            for (RestoredTable table : restoredTables) {
                ZipEntry tableEntry = zip.getEntry("tables/" + table.name() + ".bin");
                try (DataInputStream rows = new DataInputStream(
                        new BufferedInputStream(zip.getInputStream(tableEntry)))) {
                    restoreRows(connection, rows, table);
                    if (rows.read() != -1) throw invalidBackup();
                }
            }
            validateEvidenceReferences(connection, zip);
            enableConstraints(connection);
            constraintsDisabled = false;
            connection.commit();
            committed = true;
        } catch (IOException | SQLException | RuntimeException error) {
            connection.rollback();
            throw error;
        } finally {
            if (constraintsDisabled) {
                try {
                    enableConstraints(connection);
                } catch (SQLException error) {
                    if (committed) throw error;
                }
            }
        }
    }

    private RestoredTable readTableHeader(DataInputStream input) throws IOException {
        String name = input.readUTF();
        if (!safeIdentifier(name)) throw invalidBackup();
        int count = input.readInt();
        if (count < 1 || count > MAX_COLUMNS) throw invalidBackup();
        List<ColumnInfo> columns = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            String columnName = input.readUTF();
            int sqlType = input.readInt();
            String typeName = input.readUTF();
            if (!safeIdentifier(columnName)) throw invalidBackup();
            columns.add(new ColumnInfo(columnName, sqlType, typeName));
        }
        return new RestoredTable(name, columns);
    }

    private void restoreRows(Connection connection, DataInputStream input, RestoredTable table)
            throws IOException, SQLException {
        String columnList = quotedColumns(connection, table.columns());
        String placeholders = String.join(",", java.util.Collections.nCopies(table.columns().size(), "?"));
        String sql = "INSERT INTO " + quote(connection, table.name())
                + " (" + columnList + ") VALUES (" + placeholders + ")";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            while (input.readBoolean()) {
                for (int index = 0; index < table.columns().size(); index++) {
                    ColumnInfo column = table.columns().get(index);
                    if (!input.readBoolean()) {
                        statement.setNull(index + 1, column.sqlType());
                    } else {
                        int length = input.readInt();
                        if (length < 0 || length > MAX_VALUE_BYTES) throw invalidBackup();
                        byte[] bytes = input.readNBytes(length);
                        if (bytes.length != length) throw invalidBackup();
                        statement.setObject(index + 1, decodeValue(bytes, column.sqlType()));
                    }
                }
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private Object decodeValue(byte[] bytes, int sqlType) throws IOException {
        String value = new String(bytes, StandardCharsets.UTF_8);
        try {
            return switch (sqlType) {
                case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB ->
                        Base64.getDecoder().decode(value);
                case Types.BIT, Types.BOOLEAN -> value.equals("1") || Boolean.parseBoolean(value);
                case Types.TINYINT, Types.SMALLINT, Types.INTEGER -> Integer.valueOf(value);
                case Types.BIGINT -> Long.valueOf(value);
                case Types.FLOAT, Types.REAL -> Float.valueOf(value);
                case Types.DOUBLE -> Double.valueOf(value);
                case Types.NUMERIC, Types.DECIMAL -> new java.math.BigDecimal(value);
                case Types.DATE -> java.sql.Date.valueOf(value);
                case Types.TIME -> java.sql.Time.valueOf(value);
                case Types.TIME_WITH_TIMEZONE -> java.time.OffsetTime.parse(value);
                case Types.TIMESTAMP -> java.sql.Timestamp.valueOf(value.replace('T', ' '));
                case Types.TIMESTAMP_WITH_TIMEZONE -> java.time.OffsetDateTime.parse(value);
                default -> value;
            };
        } catch (IllegalArgumentException error) {
            throw new IOException("Backup contains an invalid value.", error);
        }
    }

    private void validateAndStageEvidence(ZipFile zip, Path staging) throws IOException {
        var entries = zip.entries();
        Set<String> seen = new HashSet<>();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory() || entry.getName().equals(DATABASE_ENTRY)) continue;
            if (!entry.getName().startsWith("evidence/")) {
                boolean binaryTable = entry.getName().matches("tables/[A-Za-z0-9_]+\\.bin");
                boolean readableTable = entry.getName().matches("donnees-csv/[A-Za-z0-9_]+\\.csv");
                if (!binaryTable && !readableTable) throw invalidBackup();
                continue;
            }
            String key = entry.getName().substring("evidence/".length());
            if (!key.matches("[a-fA-F0-9-]{36}\\.(jpg|png|webp|mp4|mov|webm)")
                    || !seen.add(key)) {
                throw invalidBackup();
            }
            Path target = staging.resolve(key).normalize();
            if (!target.getParent().equals(staging)) throw invalidBackup();
            try (var input = zip.getInputStream(entry)) {
                try (var output = Files.newOutputStream(target)) {
                    byte[] buffer = new byte[8192];
                    long count = 0;
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        count += read;
                        if (count > EvidenceStorageService.MAX_FILE_SIZE) {
                            throw new ResponseStatusException(
                                    HttpStatus.PAYLOAD_TOO_LARGE, "An evidence file exceeds 50 MB.");
                        }
                        output.write(buffer, 0, read);
                    }
                }
            }
        }
    }

    private void synchronizeEvidenceFiles(Path staging) throws IOException {
        Set<String> referenced = evidenceAttachments.findAll().stream()
                .map(attachment -> attachment.getStorageKey())
                .collect(java.util.stream.Collectors.toSet());
        for (Path existing : evidenceStorage.listFiles()) {
            if (!referenced.contains(existing.getFileName().toString())) {
                Files.deleteIfExists(existing);
            }
        }
    }

    private void installStagedEvidence(Path staging) throws IOException {
        try (var files = Files.list(staging)) {
            for (Path staged : files.toList()) {
                evidenceStorage.storeBackupFile(staged.getFileName().toString(), staged);
            }
        }
    }

    private void validateTableEntries(ZipFile zip, List<RestoredTable> tables) {
        Set<String> expected = tables.stream()
                .map(table -> "tables/" + table.name() + ".bin")
                .collect(java.util.stream.Collectors.toSet());
        for (String entry : expected) {
            ZipEntry actual = zip.getEntry(entry);
            if (actual == null || actual.isDirectory()) throw invalidBackup();
        }
        var entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.getName().startsWith("tables/") && !expected.contains(entry.getName())) {
                throw invalidBackup();
            }
        }
    }

    private List<TableInfo> tables(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String catalog = connection.getCatalog();
        String schema = null;
        try {
            schema = connection.getSchema();
        } catch (SQLException ignored) {
            // Some JDBC drivers do not implement getSchema().
        }
        List<TableInfo> tables = new ArrayList<>();
        try (ResultSet result = metadata.getTables(catalog, schema, "%", new String[] {"TABLE"})) {
            while (result.next()) {
                String name = result.getString("TABLE_NAME");
                String tableSchema = result.getString("TABLE_SCHEM");
                if (!safeIdentifier(name) || "INFORMATION_SCHEMA".equalsIgnoreCase(tableSchema)
                        || "mysql".equalsIgnoreCase(tableSchema) || "performance_schema".equalsIgnoreCase(tableSchema)) {
                    continue;
                }
                List<ColumnInfo> columns = new ArrayList<>();
                try (ResultSet columnRows = metadata.getColumns(catalog, tableSchema, name, "%")) {
                    while (columnRows.next()) {
                        columns.add(new ColumnInfo(
                                columnRows.getString("COLUMN_NAME"),
                                columnRows.getInt("DATA_TYPE"),
                                columnRows.getString("TYPE_NAME"),
                                columnRows.getInt("ORDINAL_POSITION")));
                    }
                }
                columns.sort(java.util.Comparator.comparingInt(column -> column.ordinal()));
                if (!columns.isEmpty()) tables.add(new TableInfo(name, tableSchema, columns));
            }
        }
        tables.sort(java.util.Comparator.comparing(TableInfo::name));
        if (tables.size() > MAX_TABLES) throw new SQLException("The application database has too many tables.");
        return tables;
    }

    private boolean sameColumns(TableInfo actual, List<ColumnInfo> backup) {
        if (actual.columns().size() != backup.size()) return false;
        for (int index = 0; index < backup.size(); index++) {
            if (!actual.columns().get(index).name().equals(backup.get(index).name())
                    || actual.columns().get(index).sqlType() != backup.get(index).sqlType()) return false;
        }
        return true;
    }

    private void disableConstraints(Connection connection) throws SQLException {
        String product = connection.getMetaData().getDatabaseProductName().toLowerCase();
        try (var statement = connection.createStatement()) {
            if (product.contains("h2")) statement.execute("SET REFERENTIAL_INTEGRITY FALSE");
            else if (product.contains("mysql") || product.contains("mariadb")) {
                statement.execute("SET FOREIGN_KEY_CHECKS=0");
            } else throw new ResponseStatusException(
                    HttpStatus.NOT_IMPLEMENTED, "Database restore is not supported for this database engine.");
        }
    }

    private void enableConstraints(Connection connection) throws SQLException {
        String product = connection.getMetaData().getDatabaseProductName().toLowerCase();
        try (var statement = connection.createStatement()) {
            if (product.contains("h2")) statement.execute("SET REFERENTIAL_INTEGRITY TRUE");
            else if (product.contains("mysql") || product.contains("mariadb")) {
                statement.execute("SET FOREIGN_KEY_CHECKS=1");
            }
        }
    }

    private boolean compatibleProducts(String source, String target) {
        return source.equalsIgnoreCase(target)
                || ((source.toLowerCase().contains("mysql") || source.toLowerCase().contains("mariadb"))
                        && (target.toLowerCase().contains("mysql") || target.toLowerCase().contains("mariadb")));
    }

    private String quote(Connection connection, String identifier) throws SQLException {
        String quote = connection.getMetaData().getIdentifierQuoteString().trim();
        return quote.isEmpty() ? identifier : quote + identifier.replace(quote, quote + quote) + quote;
    }

    private String quotedColumns(Connection connection, List<ColumnInfo> columns) throws SQLException {
        List<String> names = new ArrayList<>();
        for (ColumnInfo column : columns) names.add(quote(connection, column.name()));
        return String.join(",", names);
    }

    private boolean safeIdentifier(String value) {
        return value != null && value.matches("[A-Za-z0-9_]+");
    }

    private ResponseStatusException invalidBackup() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Backup file is invalid or incomplete.");
    }

    private List<String> evidenceKeys(Connection connection) throws SQLException {
        List<String> keys = new ArrayList<>();
        try (var statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("SELECT storage_key FROM evidence_attachments")) {
            while (rows.next()) keys.add(rows.getString(1));
        } catch (SQLException error) {
            if (error.getMessage() == null || !error.getMessage().toLowerCase().contains("evidence_attachments")) {
                throw error;
            }
        }
        return keys;
    }

    private void validateEvidenceReferences(Connection connection, ZipFile zip) throws SQLException {
        try (var statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("SELECT storage_key FROM evidence_attachments")) {
            while (rows.next()) {
                ZipEntry file = zip.getEntry("evidence/" + rows.getString(1));
                if (file == null || file.isDirectory()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Backup is missing a referenced evidence file.");
                }
            }
        }
    }

    private void deleteDirectoryContents(Path directory) throws IOException {
        try (var paths = Files.list(directory)) {
            for (Path path : paths.toList()) Files.deleteIfExists(path);
        }
    }

    private record ColumnInfo(String name, int sqlType, String typeName, int ordinal) {
        ColumnInfo(String name, int sqlType, String typeName) {
            this(name, sqlType, typeName, 0);
        }
    }

    private record TableInfo(String name, String schema, List<ColumnInfo> columns) {
    }

    private record RestoredTable(String name, List<ColumnInfo> columns) {
    }
}
