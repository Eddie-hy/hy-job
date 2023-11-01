package com.hy.job.admin.core.model;

import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class XxlJobUser {    //用户类对应表user

    private int id;
    private String username;		// 账号
    private String password;		// 密码
    private int role;				// 角色：0-普通用户、1-管理员
    private String permission;	// 权限：执行器ID列表，多个逗号分割


    //应该是判断权限作用的
    public boolean validPermission(int jobGroup){
        if (this.role == 1) {
            return true;
        } else {
            if (StringUtils.hasText(this.permission)) {
                for (String permissionItem : this.permission.split(",")) {
                    if (String.valueOf(jobGroup).equals(permissionItem)) {
                        return true;
                    }
                }
            }
            return false;
        }

    }

}
