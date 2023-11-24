package com.jiaruiblog.xuandecenter.sse;


import com.jiaruiblog.xuandecenter.thread.LogSubscribeRunnable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;

/**
 * 数据管理器用于管理Server-Sent Events (SSE) 的订阅和数据推送。
 * @ClassName DataManager
 * @Description TODO
 * @Author luojiarui
 * @Date 2023/11/23 16:35
 * @Version 1.0
 **/
@Component
public class DataManager {

    private final Map<String, List<SseEmitter>> dataEmitters = new HashMap<>();

    private final Map<String, LogSubscribeRunnable> logSubscribeRunnableMap = new LinkedHashMap<>();

    /**
     * 订阅特定数据类型的SSE连接。
     *
     * @param dataType 要订阅的数据类型
     * @param emitter  SSE连接
     */
    public void subscribe(String dataType, SseEmitter emitter) {
        dataEmitters.computeIfAbsent(dataType, k -> new ArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(dataType, emitter));
        emitter.onTimeout(() -> removeEmitter(dataType, emitter));

        // 如果当前不存在此线程，则新建线程
        if (!logSubscribeRunnableMap.containsKey(dataType)) {
            LogSubscribeRunnable logSubscribeRunnable = new LogSubscribeRunnable(dataEmitters.get(dataType));
            if (logSubscribeRunnable.exit) {
                // 使发射完成
                emitter.complete();
            } else {
                logSubscribeRunnable.run();
                logSubscribeRunnableMap.put(dataType, logSubscribeRunnable);
            }
        }
    }

    /**
     * 推送特定数据类型的数据给所有已订阅的连接。
     *
     * @param dataType 要推送的数据类型
     * @param data     要推送的数据
     */
    public void pushData(String dataType, String data) {
        List<SseEmitter> emitters = dataEmitters.getOrDefault(dataType, new ArrayList<>());
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().data(data, MediaType.TEXT_PLAIN));
            } catch (IOException e) {
                removeEmitter(dataType, emitter);
            }
        });
    }

    private void removeEmitter(String dataType, SseEmitter emitter) {
        List<SseEmitter> emitters = dataEmitters.get(dataType);
        if (emitters != null) {
            emitters.remove(emitter);
        }
        // 退出数据订阅
        exitSubscribe(dataType);
    }

    private void exitSubscribe(String dataType ) {
        LogSubscribeRunnable logSubscribeRunnable = logSubscribeRunnableMap.get(dataType);
        logSubscribeRunnable.exit = true;
        logSubscribeRunnableMap.remove(dataType);
    }
}
