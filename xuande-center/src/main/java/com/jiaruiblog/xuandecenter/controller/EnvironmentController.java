package com.jiaruiblog.xuandecenter.controller;

import com.jiaruiblog.xuandecenter.sse.DataManager;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;

/**
 * @author xiaobo
 */
@CrossOrigin
@RestController
@RequestMapping("/environment")
public class EnvironmentController {

    @Resource    private DataManager dataManager;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        // 设置的是超时时间，小于这个时间，服务端无法感知高客户端的情况
        // 参考代码：https://blog.csdn.net/u014280894/article/details/130149116
        SseEmitter emitter = new SseEmitter((long) (60000 * 5));
        dataManager.subscribe("environment", emitter);
        return emitter;
    }

    // 示例：推送环境监测数据给前端
    @GetMapping("/push/{testText}")
    public ResponseEntity<String> pushEnvironmentData(@PathVariable String testText) {
        dataManager.pushData("environment", testText);
        return ResponseEntity.ok("Data pushed successfully.");
    }
}