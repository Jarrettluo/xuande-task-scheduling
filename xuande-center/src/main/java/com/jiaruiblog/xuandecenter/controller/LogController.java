package com.jiaruiblog.xuandecenter.controller;

import com.jiaruiblog.xuandecenter.service.impl.LogServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName LogController
 * @Description TODO
 * @Author luojiarui
 * @Date 2023/11/18 12:23
 * @Version 1.0
 **/
@RestController
@RequestMapping("/log")
public class LogController {

    @Autowired
    LogServiceImpl logService;

    @GetMapping("start")
    public String startLog() throws Exception {
        logService.insertData();
        return "success";
    }

    @GetMapping("monitor")
    public String monitorLog() {
        return "abc";
    }
}
