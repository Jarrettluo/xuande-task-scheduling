package com.jiaruiblog.xuandecenter.thread;

import com.jiaruiblog.xuandecenter.entity.XuandeLog;
import com.taosdata.jdbc.tmq.ConsumerRecord;
import com.taosdata.jdbc.tmq.ConsumerRecords;
import com.taosdata.jdbc.tmq.TMQConstants;
import com.taosdata.jdbc.tmq.TaosConsumer;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * 同一个主题向多个emitter进行发送数据
 * @ClassName LogSubscribeRunnable
 * @Description TODO
 * @Author luojiarui
 * @Date 2023/11/23 16:51
 * @Version 1.0
 **/
public class LogSubscribeRunnable implements Runnable{

    // 线程退出标志
    public volatile boolean exit = false;

    private String topic;

    private Connection connection;

    // 存储了队列
    private List<SseEmitter> sseEmitterList = new ArrayList<>();

    public LogSubscribeRunnable(List<SseEmitter> sseEmitterList, String topic) {
        this.sseEmitterList = sseEmitterList;
        this.topic = topic;
        // 连接
        prepareConnection();
        if (this.connection == null) {
            this.exit = true;
            return;
        }
        // 建立主题
        createTopic();
    }

    @Override
    public void run() {
        // 参数校验
        getSubscribe();
        // 删除主题
        deleteTopic();
    }

    private void prepareConnection () {
        try {
            // prepare
            Class.forName("com.taosdata.jdbc.TSDBDriver");
            String jdbcUrl = "jdbc:TAOS://127.0.0.1:6030/?user=root&password=taosdata";
            this.connection = DriverManager.getConnection(jdbcUrl);
        } catch (SQLException | ClassNotFoundException e) {
            this.exit = true;
            e.printStackTrace();
        }
    }

    private void createTopic() {
        try (Statement statement = this.connection.createStatement()) {
            statement.executeUpdate("drop topic if exists " + this.topic);
            statement.executeUpdate("create topic " + this.topic + " AS SELECT ts, `value` FROM xuande_log.t_log");
        } catch (Exception e) {
            this.exit = true;
            e.printStackTrace();
        }
    }

    public void getSubscribe() {
        try {
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
                consumer.subscribe(Collections.singletonList(this.topic));
                while (!sseEmitterList.isEmpty() && !exit) {
                    System.out.println("当前等待连接中" + sseEmitterList.size());
                    ConsumerRecords<XuandeLog> meters = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<XuandeLog> r : meters) {
                        XuandeLog meter = r.value();
                        pushData(this.topic, meter.toString());
                    }
                }
                consumer.unsubscribe();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     * 推送特定数据类型的数据给所有已订阅的连接。
     *
     * @param dataType 要推送的数据类型
     * @param data     要推送的数据
     */
    private void pushData(String dataType, String data) {
        List<SseEmitter> errorEmitter = new ArrayList<>();
        sseEmitterList.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().data(data, MediaType.TEXT_PLAIN));
            } catch (IOException e) {
                errorEmitter.add(emitter);
            }
        });
        sseEmitterList.removeAll(errorEmitter);
    }

    private void deleteTopic() {
        try (Statement statement = this.connection.createStatement()) {
            statement.executeUpdate("drop topic if exists " + this.topic);
        } catch (Exception e) {
            this.exit = true;
            e.printStackTrace();
        }
    }

}
