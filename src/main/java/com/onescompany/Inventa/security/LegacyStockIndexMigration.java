package com.onescompany.Inventa.security;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class LegacyStockIndexMigration implements ApplicationRunner {
    private final DataSource dataSource;

    public LegacyStockIndexMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String table = findTable(metadata);
            if (table == null) {
                return;
            }

            Map<String, List<String>> uniqueIndexes = new LinkedHashMap<>();
            try (ResultSet indexes = metadata.getIndexInfo(connection.getCatalog(), null, table, true, false)) {
                while (indexes.next()) {
                    String indexName = indexes.getString("INDEX_NAME");
                    String columnName = indexes.getString("COLUMN_NAME");
                    if (indexName != null && columnName != null) {
                        uniqueIndexes.computeIfAbsent(indexName, ignored -> new java.util.ArrayList<>())
                                .add(columnName.toLowerCase(java.util.Locale.ROOT));
                    }
                }
            }
            for (Map.Entry<String, List<String>> index : uniqueIndexes.entrySet()) {
                if (index.getValue().equals(List.of("name"))) {
                    dropIndex(connection, metadata, table, index.getKey());
                }
            }
        }
    }

    private String findTable(DatabaseMetaData metadata) throws SQLException {
        try (ResultSet tables = metadata.getTables(null, null, null, new String[] { "TABLE" })) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                if ("stock_items".equalsIgnoreCase(tableName)) {
                    return tableName;
                }
            }
        }
        return null;
    }

    private void dropIndex(Connection connection, DatabaseMetaData metadata, String table, String index)
            throws SQLException {
        String quote = metadata.getIdentifierQuoteString().trim();
        if (quote.isEmpty()) {
            quote = "";
        }
        String quotedIndex = quote.isEmpty() ? index : quote + index + quote;
        String sql = "MySQL".equalsIgnoreCase(metadata.getDatabaseProductName())
                ? "ALTER TABLE " + quote + table + quote + " DROP INDEX " + quotedIndex
                : "DROP INDEX " + quotedIndex;
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
