package com.gigaspaces.micrometer.hsqldb;

import io.micrometer.core.instrument.step.StepRegistryConfig;

import java.time.Duration;
import java.util.Properties;

import static io.micrometer.core.instrument.config.validate.PropertyValidator.getDuration;

public class HsqldbRegistryConfig implements StepRegistryConfig {

    static HsqldbRegistryConfig DEFAULT = new HsqldbRegistryConfig();
    private static final String DEFAULT_DRIVER_CLASS_NAME = "org.hsqldb.jdbc.JDBCDriver";
    private static final String DEFAULT_DBTYPE_STRING = "VARCHAR(300)";

    private String dbName;
    private String username;
    private String password;
    private String host;
    private String port;
    private String driverClassName;
    private String dbTypeString;

    public HsqldbRegistryConfig(){

    }

    public HsqldbRegistryConfig( Properties properties ){
        setDbName(properties.getProperty("dbname"));
        setDriverClassName(properties.getProperty("driverClassName", DEFAULT_DRIVER_CLASS_NAME));
        setHost(properties.getProperty("host"));
        setPort( properties.getProperty("port") );
        setUsername(properties.getProperty("username"));
        setPassword(properties.getProperty("password"));
        setDbTypeString(properties.getProperty("dbTypeString", DEFAULT_DBTYPE_STRING));
    }

    @Override
    public String prefix() {
        return "hsqldb";
    }

    @Override
    public String get(String key) {
        return null;
    }

    @Override
    public Duration step() {
        return Duration.ofSeconds(5);
    }

    public String getConnectionUrl() {
        return "jdbc:hsqldb:hsql://" + host + ":" + port + "/" + dbName;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDbTypeString() {
        return dbTypeString;
    }

    public void setDbTypeString(String dbTypeString) {
        this.dbTypeString = dbTypeString;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

}
