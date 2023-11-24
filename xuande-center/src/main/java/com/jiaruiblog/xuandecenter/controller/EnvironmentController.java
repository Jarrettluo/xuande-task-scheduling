package com.jiaruiblog.xuandecenter.controller;

import com.jiaruiblog.xuandecenter.sse.DataManager;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;

/**
 * @author xiaobo
 */
@RestController
@RequestMapping("/environment")
public class EnvironmentController {

    @Resource    private DataManager dataManager;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter();
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