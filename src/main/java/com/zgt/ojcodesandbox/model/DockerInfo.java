package com.zgt.ojcodesandbox.model;


import lombok.Data;

@Data
public class DockerInfo {
    // 容器id
    String containerId;
    // 使用次数
    long runCount;
    // 是否存活
    boolean running;
    // 运行信息 Status
    String status;
    // 运行状态 state
    String state;
    // 镜像
    String ImageName;
    // 最后调用的时间戳
    long lastExecTime;
}
