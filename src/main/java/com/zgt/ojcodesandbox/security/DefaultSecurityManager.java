package com.zgt.ojcodesandbox.security;

import java.security.Permission;

public class DefaultSecurityManager extends SecurityManager{

    // 检测所有权限
    @Override
    public void checkPermission(Permission perm) {
        System.out.println("默认不做任何权限校验");
        super.checkPermission(perm);
    }
}
