package com.zgt.ojcodesandbox.service;

import com.zgt.ojcodesandbox.model.DockerInfo;

import java.util.List;


public interface DockerService {

    // 根据镜像名称 ，获取docker容器列表
    public List<DockerInfo> getDockerInfo(String imageName);

    // 新建容器
    public DockerInfo createDockerContainer(String imageName);

    // 删除容器
    public void deleteDockerContainer(DockerInfo dockerInfo);

    // 使用时间戳来进行最近最久未使用算法的实现，返回对应的容器Id
    public String getContainerIdByLRU();
}
