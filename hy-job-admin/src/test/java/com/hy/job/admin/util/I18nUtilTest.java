package com.hy.job.admin.util;

import com.hy.job.admin.core.util.I18nUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @Author: HY
 * @Date: 2023-10-04-19:55
 * @Description:
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class I18nUtilTest {

    private static Logger logger = LoggerFactory.getLogger(I18nUtilTest.class);

    @Test
    public void test(){
        logger.info(I18nUtil.getString("admin_name"));
        logger.info(I18nUtil.getMultString("admin_name", "admin_name_full"));
        logger.info(I18nUtil.getMultString());
    }

    @Test
    public void Cron(){

    }
}
