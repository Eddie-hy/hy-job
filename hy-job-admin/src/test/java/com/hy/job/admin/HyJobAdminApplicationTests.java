package com.hy.job.admin;

import com.hy.job.admin.core.model.XxlJobUser;
import com.hy.job.admin.service.LoginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HyJobAdminApplicationTests {
    @Autowired
    LoginService loginService;

    @Test
    public void test(){
        XxlJobUser xxlJobUser = new XxlJobUser();
        xxlJobUser.setId(1);
        xxlJobUser.setPassword("123456");
        xxlJobUser.setPermission("fsadfa");
        xxlJobUser.setRole(0);




    }

}
