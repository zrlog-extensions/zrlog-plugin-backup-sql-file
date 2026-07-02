package com.zrlog.plugin.backup.service;

import com.hibegin.common.dao.DAO;
import com.hibegin.common.dao.DataSourceWrapperImpl;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.message.DbPropertiesResponse;
import com.zrlog.plugin.type.ActionType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class SiteExportDatabase {

    private static volatile boolean configured;

    private SiteExportDatabase() {
    }

    public static void ensureConfigured(IOSession session) {
        if (configured) {
            return;
        }
        synchronized (SiteExportDatabase.class) {
            if (configured) {
                return;
            }
            DbPropertiesResponse response = session.getResponseSync(ContentType.JSON, new HashMap<>(), ActionType.GET_DB_PROPERTIES, DbPropertiesResponse.class);
            String path = response == null ? "" : Objects.toString(response.getDbProperties(), "");
            if (path.trim().isEmpty()) {
                throw new IllegalStateException("db.properties path is empty");
            }
            File dbPropertiesFile = new File(path);
            if (!dbPropertiesFile.exists()) {
                throw new IllegalStateException("db.properties not found: " + path);
            }
            Properties properties = loadProperties(dbPropertiesFile);
            DataSourceWrapperImpl dataSource = new DataSourceWrapperImpl(properties, false);
            if (!dataSource.isWebApi()) {
                String driverClass = properties.getProperty("driverClass");
                if (driverClass != null && !driverClass.trim().isEmpty()) {
                    dataSource.setDriverClassName(driverClass);
                }
                dataSource.setJdbcUrl(properties.getProperty("jdbcUrl"));
            }
            dataSource.setUsername(properties.getProperty("user"));
            dataSource.setPassword(properties.getProperty("password"));
            DAO.setDs(dataSource);
            configured = true;
        }
    }

    public static List<Map<String, Object>> queryList(IOSession session, String sql, Object... params)
            throws SQLException {
        ensureConfigured(session);
        return new DAO().queryListWithParams(sql, params);
    }

    public static Map<String, Object> queryFirst(IOSession session, String sql, Object... params)
            throws SQLException {
        ensureConfigured(session);
        return new DAO().queryFirstWithParams(sql, params);
    }

    public static Object queryFirstObj(IOSession session, String sql, Object... params) throws SQLException {
        ensureConfigured(session);
        return new DAO().queryFirstObj(sql, params);
    }

    public static String queryString(IOSession session, String sql, Object... params) throws SQLException {
        Object value = queryFirstObj(session, sql, params);
        return value == null ? "" : Objects.toString(value, "");
    }

    private static Properties loadProperties(File dbPropertiesFile) {
        Properties dbProperties = new Properties();
        try (FileInputStream in = new FileInputStream(dbPropertiesFile)) {
            dbProperties.load(in);
            return dbProperties;
        } catch (IOException e) {
            throw new IllegalStateException("read db.properties failed: " + dbPropertiesFile, e);
        }
    }
}
