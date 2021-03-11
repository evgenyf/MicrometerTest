package com.gigaspaces.micrometer.hsqldb;

import com.gigaspaces.metrics.MetricTagsSnapshot;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import org.hsqldb.error.ErrorCode;

import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class HsqldbMetricsRegistry extends StepMeterRegistry {

    private final Map<String,PreparedStatement> _preparedStatements = new HashMap<>();

    public HsqldbMetricsRegistry(HsqldbRegistryConfig config, Clock clock) {
        super(config, clock);
        start(new NamedThreadFactory("hsqldb-metrics-publisher"));
    }

    @Override
    protected void publish() {
        long timestamp = System.currentTimeMillis();
        getMeters().stream().forEach(meter -> {
            Meter.Id id = meter.getId();
            String name = id.getName();
            Meter.Type type = id.getType();
            String tableName = getTableName( name );
            double value = Double.NaN;
            if( type.equals( Meter.Type.GAUGE ) ){
                Gauge gauge = ( Gauge )meter;
                value = gauge.value();
            }
            else if( type.equals( Meter.Type.COUNTER ) ){
                Counter counter = (Counter)meter;
                value = counter.count();
            }

            List<Object> values = new ArrayList<>();
            List<String> columns = new ArrayList<>();

            String insertSQL = generateInsertQuery(tableName, timestamp, value, id.getTags(), values, columns);

            System.out.println("Publishing " + id + ", name=" + name + ", type=" + type + ", value=" + value + ", tableName=" + tableName);
            System.out.println( insertSQL );
        } );
    }

    private String generateInsertQuery(String tableName, long timestamp, Object value, List<Tag> tags, List<Object> values, List<String> columnsList ) {
        StringJoiner columns = new StringJoiner(",");
        StringJoiner parameters = new StringJoiner(",");

        addColumnNameAndValue( "TIME", new Timestamp(timestamp), columns, columnsList, parameters, values );

/*
        PredefinedSystemMetrics predefinedSystemMetrics = PredefinedSystemMetrics.valueOf(tableName);
        List<String> columnForInsert = predefinedSystemMetrics.getColumns();
*/
/*        tags.getTags().forEach((k, v) -> {
            if( columnForInsert == null || columnForInsert.contains( k ) ){
                addColumnNameAndValue( k, v, columns, columnsList, parameters, values );
                if( k == null ){
                    _logger.warn( "Null column name using while inserting row into table {}", tableName );
                }
                if( v == null ){
                    _logger.warn( "Null [{}] value using while inserting row into table {}", k, tableName );
                }
            }
        });*/

/*
        if( predefinedSystemMetrics == PredefinedSystemMetrics.JVM_MEMORY_HEAP_USED_BYTES ||
                predefinedSystemMetrics == PredefinedSystemMetrics.JVM_MEMORY_HEAP_USED_PERCENT ||
                predefinedSystemMetrics == PredefinedSystemMetrics.PROCESS_CPU_USED_PERCENT ){

            addMissingNullColumnsToVmMetricsIfNeeded( values, columns, parameters, tags.getTags(), columnsList );
        }
*/

        addColumnNameAndValue( "VALUE", value, columns, columnsList, parameters, values );
        if( value == null ) {
            System.out.println("Null VALUE using while inserting row into table " + tableName);
        }

        String result = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + parameters + ")";
        //System.out.println("Generated insert query: " + result);
        return result;
    }

    private PreparedStatement handleGetOrCreatePreparedStatement(Object value, String tableName, Connection connection, String insertSQL) throws SQLException {
        PreparedStatement statement;
        try {
            statement = getOrCreatePreparedStatement(insertSQL, connection);

        } catch (SQLSyntaxErrorException e) {
            System.out.println("Report to " + tableName + " failed due to " + e.getMessage());
            if (e.getErrorCode() == -(ErrorCode.X_42501)) {
                //indicates that we should create a table - since GS14322
                //createTable(connection, tableName, value);
                statement = getOrCreatePreparedStatement(insertSQL, connection);
            } else {
                throw e;
            }
        }
        return statement;
    }

/*    private void createTable(Connection con, String tableName, Object value) {
        try (Statement statement = con.createStatement()) {
            String sqlCreateTable = generateCreateTableQuery(tableName, value);
            statement.executeUpdate(sqlCreateTable);
            System.out.println("Table [" + tableName + "] successfully created");

            //String sqlCreateIndex = generateCreateIndexQuery(tableName);
            //statement.executeUpdate(sqlCreateIndex);
            //System.out.println("Index for table [" + tableName + "] successfully created");
        } catch (SQLException e) {
            System.out.println("Failed to create table " + tableName + " due to " + e.toString());
        }
    }*/
    /*

    private String generateCreateTableQuery(String tableName, Object value) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE CACHED TABLE ").append(tableName).append(" (");

        PredefinedSystemMetrics predefinedSystemMetrics = PredefinedSystemMetrics.valueOf(tableName);
        List<String> columns = predefinedSystemMetrics.getColumns();

        columns.forEach(columnName -> {
            sb.append(columnName).append(' ').append(getDbType(TableColumnTypesEnum.getJDBCType(columnName))).append(',');
        });

        sb.append("VALUE ").append(getDbType(value));
        sb.append(')');

        String result = sb.toString();
        System.out.println("create table query:" + result);
        return result;
    }

*/

    private PreparedStatement getOrCreatePreparedStatement(String sql, Connection connection) throws SQLException {
        PreparedStatement statement = _preparedStatements.get(sql);
        if (statement == null) {
            statement = connection.prepareStatement(sql);
            _preparedStatements.put(sql, statement);
        }
        return statement;
    }

    private void addColumnNameAndValue( String columnName, Object value, StringJoiner columns,
                                        List<String> columnsList, StringJoiner parameters, List<Object> values ) {
        columns.add( columnName );
        columnsList.add( columnName );
        parameters.add("?");
        values.add( value );
    }

    @Override
    protected TimeUnit getBaseTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }

    private String getTableName( String metricName ){
        return metricName.replace( '.', '_' ).toUpperCase();
    }
}