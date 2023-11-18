package com.jiaruiblog.xuandecenter.service.impl;

import com.jiaruiblog.xuandecenter.service.LogService;
import com.taosdata.jdbc.TSDBDriver;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

/**
 * @ClassName LogServiceImpl
 * @Description TODO
 * @Author luojiarui
 * @Date 2023/11/18 12:23
 * @Version 1.0
 **/
@Service
public class LogServiceImpl implements LogService {



    public Connection getConn() throws Exception{
        Class.forName("com.taosdata.jdbc.TSDBDriver");
        String jdbcUrl = "jdbc:TAOS://localhost:6030/xuande_log?user=root&password=taosdata";
        Properties connProps = new Properties();
        connProps.setProperty(TSDBDriver.PROPERTY_KEY_CHARSET, "UTF-8");
        connProps.setProperty(TSDBDriver.PROPERTY_KEY_LOCALE, "en_US.UTF-8");
        connProps.setProperty(TSDBDriver.PROPERTY_KEY_TIME_ZONE, "UTC-8");
        connProps.setProperty("debugFlag", "135");
        connProps.setProperty("maxSQLLength", "1048576");
        Connection conn = DriverManager.getConnection(jdbcUrl, connProps);
        return conn;
    }

    public void insertData() throws Exception {

        try (Connection connection = getConn();) {
            Statement stmt = connection.createStatement();
            // insert data
            int affectedRows = stmt.executeUpdate("insert into t_log values(now, 10.3) (now + 1s, 9.3)");

            System.out.println(affectedRows);
        } catch (Exception e) {
            e.printStackTrace();
        }



    }

}
