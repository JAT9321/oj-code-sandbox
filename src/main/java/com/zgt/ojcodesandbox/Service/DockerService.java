package com.zgt.ojcodesandbox.Service;

import com.zgt.ojcodesandbox.model.DockerInfo;
import org.springframework.stereotype.Service;

import java.util.List;


public interface DockerService {

    // 根据镜像名称 ，获取docker容器列表
    public List<DockerInfo> getDockerInfo(String imageName);

    // 新建容器
    public DockerInfo createDockerContainer(String imageName, String userCodeParentPath);

    // 删除容器
    public boolean deleteDockerContainer(DockerInfo dockerInfo);

}
