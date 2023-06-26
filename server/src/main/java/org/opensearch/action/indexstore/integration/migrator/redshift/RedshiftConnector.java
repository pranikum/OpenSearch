/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.action.indexstore.integration.migrator.redshift;

import org.opensearch.common.Strings;
import org.opensearch.common.util.CollectionUtils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class RedshiftConnector {
    private static final String REDSHIFT_JDBC_ENDPOINT = "jdbc:redshift://redshift-hackathon-cluster-1.c8nyoqxrpmls.ap-south-1.redshift.amazonaws.com:5439/dev";
    private static final String REDSHIFT_USER_NAME = "hackathonuser";
    private static final String REDSHIFT_USER_PPWD = "hackath0nPwd";

    public Connection getConnection() {
        Connection conn1 = null;
        try {
            Class.forName("com.amazon.redshift.jdbc42.Driver");
            Properties props = new Properties();
            props.setProperty("user", REDSHIFT_USER_NAME);
            props.setProperty("password", REDSHIFT_USER_PPWD);
            conn1 = DriverManager.getConnection(REDSHIFT_JDBC_ENDPOINT, props);
            if (conn1 != null) {
                System.out.println("Connected with connection #1");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn1;
    }

    public void createTable(Connection connection, String tableName, List<String> fields, Map<String, Object> valueMap) throws SQLException {
        Statement statement = null;
        System.out.println("Creating Table with table name.." + tableName);
        String createTableQuery = buildCreateTableQuery(tableName, fields, valueMap);
        try {
            if(createTableQuery == null || createTableQuery.isBlank()) {
                return;
            }
            statement = connection.createStatement();
            statement.executeUpdate(createTableQuery);
            System.out.println("Table Created with name " + tableName);
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        } finally {
            closeStatement(statement);
        }
    }

    public void insertData(Connection connection, String tableName, List<String> fields, List<Map<String, Object>> valuesMap) throws SQLException {
        PreparedStatement pStatement = null;
        try {
            pStatement = connection.prepareStatement(buildInsertStatement(tableName, fields));
            for (Map<String, Object> valueMap : valuesMap) {
                if(valueMap.size() != fields.size()) {
                    System.out.println("Value map is " + valueMap + " fields is " + fields);
                    continue;
                }
                for(int i=0; i<fields.size(); i++) {
                    setValue(pStatement, i+1 ,valueMap.get(fields.get(i)));
                }
                pStatement.executeUpdate();
            }
            System.out.println("Total " + valuesMap.size() + " Rows Inserted in Table " + tableName);
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        } finally {
            closeStatement(pStatement);
        }
    }

    private void setValue(PreparedStatement pStatement, int index, Object value) throws SQLException {
        if(value instanceof String) {
            pStatement.setString(index, (String) value);
        } else if(value instanceof Integer) {
            pStatement.setInt(index, (Integer) value);
        } else if(value instanceof Long) {
            pStatement.setLong(index, (Long) value);
        } else if(value instanceof Double) {
            pStatement.setDouble(index, (Double) value);
        } else if(value instanceof Float) {
            pStatement.setDouble(index, (Float) value);
        } else if(value instanceof Date) {
            pStatement.setDate(index, new java.sql.Date( ((Date)value).getTime()));
        }
    }

    private void closeStatement(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private String buildInsertStatement(String tableName, List<String> fields) {
        if(tableName == null || tableName.isBlank() || fields == null  || fields.isEmpty()) {
            return null;
        }

        StringBuilder insertStatementBuilder = new StringBuilder();
        insertStatementBuilder.append("INSERT into " + tableName + "(");

        for (String field : fields) {
            insertStatementBuilder.append(field + ",");
        }
        insertStatementBuilder.deleteCharAt(insertStatementBuilder.length()-1);
        insertStatementBuilder.append(") values (");
        for (int i=0; i<fields.size(); i++) {
            insertStatementBuilder.append("?,");
        }
        insertStatementBuilder.deleteCharAt(insertStatementBuilder.length()-1);
        insertStatementBuilder.append(");");

        System.out.println("insertStatementBuilder = " + insertStatementBuilder);
        return insertStatementBuilder.toString();
    }

    private String buildCreateTableQuery(String tableName, List<String> fields, Map<String, Object> typeMap) {
        if(tableName == null || tableName.isBlank() || fields == null  || fields.isEmpty()) {
            return null;
        }

        StringBuilder createQueryBuilder = new StringBuilder();
        createQueryBuilder.append("create table if not exists " + tableName + "(");

        for (String field : fields) {
            createQueryBuilder.append(field + " " +  getType(typeMap.get(field)) + ",");
        }
        createQueryBuilder.deleteCharAt(createQueryBuilder.length()-1);
        createQueryBuilder.append(");");
        System.out.println("createQueryBuilder = " + createQueryBuilder);
        return createQueryBuilder.toString();
    }

    private String getType(Object type) {
        if(type instanceof String) {
            return "VARCHAR";
        } else if(type instanceof Integer) {
            return "INTEGER";
        } else if (type instanceof Float || type instanceof Double) {
            return "DECIMAL";
        } else if (type instanceof Date) {
            return "DATE";
        } else if (type instanceof Long) {
            return "BIGINT";
        } else if (type instanceof Boolean) {
            return "BOOLEAN";
        }
        return "VARCHAR";
    }
}
