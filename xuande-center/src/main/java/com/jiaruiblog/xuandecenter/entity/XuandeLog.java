package com.jiaruiblog.xuandecenter.entity;

import lombok.Data;

import java.sql.Timestamp;

/**
 * @ClassName XuandeLog
 * @Description TODO
 * @Author luojiarui
 * @Date 2023/11/18 13:40
 * @Version 1.0
 **/
@Data
public class XuandeLog {

    private Timestamp ts;

    private float value;
}
