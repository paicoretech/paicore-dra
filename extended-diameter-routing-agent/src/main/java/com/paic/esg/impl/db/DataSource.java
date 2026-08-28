/*
 * Extended Diameter Routing Agent
 * Copyright (C) 2019-2026 PAiCore Technology
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.paic.esg.impl.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.paic.esg.helpers.ExtendedResource;
import com.paic.esg.impl.db.entity.Realm;
import com.paic.esg.impl.db.repository.RealmRepository;
import com.paic.esg.impl.db.persistence.Column;
import com.paic.esg.impl.db.persistence.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class DataSource implements RealmRepository {

    private static final Logger logger = LoggerFactory.getLogger(DataSource.class);


    private ObjectMapper mapper;
    private Template template;
    private Connection connection;
    private static DataSource instance = null;

    private static DataSource getInstance() {
        if (instance == null) {
            try {
                instance = new DataSource();
            } catch (Exception e) {
                logger.warn("Exception caught", e);
            }
        }
        return instance;
    }

    public DataSource() throws Exception {
        mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        template = mapper.readValue(new ExtendedResource("application.yaml").getAsStream(), Template.class);

        Properties properties = new Properties();
        properties.put("user", template.getUser());
        properties.put("password", template.getPassword());

        connection = DriverManager.getConnection(template.getUrl(), properties);

    }

    public static DataSource initialize() {
        DataSource.getInstance();
        logger.info("DataSource is initializing...");
        return instance;
    }

    public <T> List<T> findByQuery(Class<T> classEntity, String query, String... params) {

        try {
            Field[] fields = classEntity.getDeclaredFields();
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(String.format(query, params));

            List<T> result = new ArrayList<>();
            while (rs.next()) {
                T entity = classEntity.newInstance();
                setValues(entity, fields, rs);
                result.add(entity);
            }
            rs.close();
            st.close();

            return result;
        } catch (Exception e) {
            logger.warn("Exception caught", e);
            return null;
        }
    }

    private void setValues(Object entity, Field[] fields, ResultSet rs) throws Exception {
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Column.class) && field.getType().getPackage().getName().equals("java.lang")) {

                field.set(entity, rs.getObject(field.getAnnotation(Column.class).name()));
            } else if (field.getType().isArray() || field.getType().getPackage().getName().equals("java.lang")) {
                Object value = rs.getObject(field.getName());
                if (value instanceof org.postgresql.jdbc.PgArray) {
                    //TODO: add other type of array
                    field.set(entity, ((org.postgresql.jdbc.PgArray) value).getArray());
                } else {
                    field.set(entity, rs.getObject(field.getName()));
                }
            } else if (!field.getType().getClass().equals(List.class)) {
                Object subEntity = field.getType().newInstance();
                Field[] subFields = subEntity.getClass().getDeclaredFields();
                setValues(subEntity, subFields, rs);
                field.set(entity, subEntity);
            }

            field.setAccessible(false);
        }
    }

    public <T> Long save(T t) {
        try {
            Table persistence = t.getClass().getAnnotation(Table.class);
            Field[] fields = t.getClass().getDeclaredFields();
            List<String> keys = new ArrayList<>();
            List<Object> vls = new ArrayList<>();
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                field.setAccessible(true);
                if (field.isAnnotationPresent(Column.class) && field.get(t) != null) {
                    keys.add(field.getAnnotation(Column.class).name());
                    vls.add(field.get(t));
                } else if ((field.getType().isArray() || field.getType().getPackage().getName().equals("java.lang")) && field.get(t) != null) {
                    keys.add(field.getName());
                    vls.add(field.get(t));
                }
                field.setAccessible(false);
            }
            String sql = "INSERT INTO " + persistence.name() + "(" + String.join(",", keys) + ") " +
                    "VALUES (?" + String.join(",?", keys.stream().map(f -> "").collect(Collectors.toList())) + ")";
            PreparedStatement pStatement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            for (int j = 0; j < vls.size(); j++) {
                Object obj = vls.get(j);
                if (obj instanceof Long) {
                    pStatement.setLong(j + 1, (Long) obj);
                } else if (obj instanceof String) {
                    pStatement.setString(j + 1, obj.toString());
                } else if (obj instanceof String[]) {
                    pStatement.setArray(j + 1, connection.createArrayOf("varchar", (Object[]) obj));
                } else if (obj instanceof Boolean) {
                    pStatement.setBoolean(j + 1, (Boolean) obj);
                } else if (obj instanceof Integer) {
                    pStatement.setInt(j + 1, (int) obj);
                } else if (obj instanceof Double) {
                    pStatement.setDouble(j + 1, (Double) obj);
                } else if (obj instanceof Float) {
                    pStatement.setFloat(j + 1, (Float) obj);
                }
            }

            if (pStatement.executeUpdate() > 0) {
                ResultSet resultSet = pStatement.getGeneratedKeys();
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
                resultSet.close();
            }
            pStatement.close();
        } catch (Exception e) {
            logger.warn("Exception caught", e);
        }
        return null;
    }

    public void executeUpdate(String query) {
        try {
            Statement st = connection.createStatement();
            st.executeUpdate(query);
            st.close();
        } catch (Exception e) {
            logger.warn("Exception caught", e);
        }
    }

    @Override
    public void saveRealm(Realm realm) {

        Long realmId = save(realm);
        Long applId = null;
        try {
            PreparedStatement pStatement = connection.prepareStatement("SELECT appl_id FROM application_id WHERE vendor_id = ? and auth_appl_id = ? and acct_appl_id = ?;");
            pStatement.setLong(1, realm.getApplicationId().getVendorId());
            pStatement.setLong(2, realm.getApplicationId().getAuthApplId());
            pStatement.setLong(3, realm.getApplicationId().getAcctApplId());
            ResultSet resultSet = pStatement.executeQuery();
            if (resultSet.next()) {
                applId = resultSet.getLong(1);
            }
            resultSet.close();
            pStatement.close();
        } catch (Exception e) {
            logger.warn("Exception caught", e);
        }
        if (applId == null) {
            applId = save(realm.getApplicationId());
        }
        executeUpdate(String.format("INSERT INTO realm_application(realm_id, appl_id) VALUES (%s, %s)", realmId, applId));
    }

    @Override
    public List<Realm> findAll() {
        return findByQuery(Realm.class, "SELECT r.*, a.* " +
                "FROM realm r JOIN realm_application ra " +
                "ON r.realm_id = ra.realm_id JOIN application_id a ON a.appl_id = ra.appl_id;");
    }

}
