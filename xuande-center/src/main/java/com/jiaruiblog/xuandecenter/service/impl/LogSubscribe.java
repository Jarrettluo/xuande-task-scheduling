package com.jiaruiblog.xuandecenter.service.impl;

import com.jiaruiblog.xuandecenter.entity.XuandeLog;
import com.taosdata.jdbc.tmq.TMQConstants;
import com.taosdata.jdbc.tmq.TaosConsumer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Collections;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;


import com.taosdata.jdbc.tmq.ConsumerRecord;
import com.taosdata.jdbc.tmq.ConsumerRecords;
import com.taosdata.jdbc.tmq.TMQConstants;
import com.taosdata.jdbc.tmq.TaosConsumer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 参考代码：https://docs.taosdata.com/develop/tmq/#%E7%A4%BA%E4%BE%8B%E4%BB%A3%E7%A0%81
 * 创建topic CREATE TOPIC tmq_topic AS SELECT ts, `value` FROM xuande_log.t_log;
 * @ClassName LogSubscribe
 * @Description TODO
 * @Author luojiarui
 * @Date 2023/11/18 13:42
 * @Version 1.0
 **/
public class LogSubscribe {

    private static final String TOPIC = "tmq_topic";
    private static final String DB_NAME = "xuande_log";
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public static void main(String[] args) {
        LogSubscribe logSubscribe = new LogSubscribe();
        logSubscribe.getSubscribe();
    }

    public void getSubscribe() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                shutdown.set(true);
            }
        }, 100_000);

        try {
            // prepare
            Class.forName("com.taosdata.jdbc.TSDBDriver");
            String jdbcUrl = "jdbc:TAOS://127.0.0.1:6030/?user=root&password=taosdata";
            Connection connection = DriverManager.getConnection(jdbcUrl);
//            try (Statement statement = connection.createStatement()) {
//                statement.executeUpdate("drop topic if exists " + TOPIC);
//                statement.executeUpdate("drop database if exists " + DB_NAME);
//                statement.executeUpdate("create database " + DB_NAME + " wal_retention_period 3600");
//                statement.executeUpdate("use " + DB_NAME);
//                statement.executeUpdate(
//                        "CREATE TABLE `meters` (`ts` TIMESTAMP, `current` FLOAT, `voltage` INT) TAGS (`groupid` INT, `location` BINARY(24))");
//                statement.executeUpdate("CREATE TABLE `d0` USING `meters` TAGS(0, 'California.LosAngles')");
//                statement.executeUpdate("INSERT INTO `d0` values(now - 10s, 0.32, 116)");
//                statement.executeUpdate("INSERT INTO `d0` values(now - 8s, NULL, NULL)");
//                statement.executeUpdate(
//                        "INSERT INTO `d1` USING `meters` TAGS(1, 'California.SanFrancisco') values(now - 9s, 10.1, 119)");
//                statement.executeUpdate(
//                        "INSERT INTO `d1` values (now-8s, 10, 120) (now - 6s, 10, 119) (now - 4s, 11.2, 118)");
//                // create topic
//                statement.executeUpdate("create topic " + TOPIC + " as select * from meters");
//            }

            // create consumer
            Properties properties = new Properties();
            properties.getProperty(TMQConstants.CONNECT_TYPE, "jni");
            properties.setProperty(TMQConstants.BOOTSTRAP_SERVERS, "127.0.0.1:6030");
            properties.setProperty(TMQConstants.CONNECT_USER, "root");
            properties.setProperty(TMQConstants.CONNECT_PASS, "taosdata");
            properties.setProperty(TMQConstants.MSG_WITH_TABLE_NAME, "true");
            properties.setProperty(TMQConstants.ENABLE_AUTO_COMMIT, "true");
            properties.setProperty(TMQConstants.AUTO_COMMIT_INTERVAL, "1000");
            properties.setProperty(TMQConstants.GROUP_ID, "test1");
            properties.setProperty(TMQConstants.CLIENT_ID, "1");
            properties.setProperty(TMQConstants.AUTO_OFFSET_RESET, "earliest");
            properties.setProperty(TMQConstants.VALUE_DESERIALIZER,
                    "com.jiaruiblog.xuandecenter.deserializer.LogDeserializer");
            properties.setProperty(TMQConstants.VALUE_DESERIALIZER_ENCODING, "UTF-8");

            // poll data
            try (TaosConsumer<XuandeLog> consumer = new TaosConsumer<>(properties)) {
                consumer.subscribe(Collections.singletonList(TOPIC));
                while (!shutdown.get()) {
                    ConsumerRecords<XuandeLog> meters = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<XuandeLog> r : meters) {
                        XuandeLog meter = r.value();
                        System.out.println(meter);
                    }
                }
                consumer.unsubscribe();
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        timer.cancel();
    }
}
