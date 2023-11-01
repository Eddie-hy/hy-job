package com.hy.job.admin.core.util;

import com.hy.job.admin.core.conf.XxlJobAdminConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @Author: HY
 * @Date: 2023-10-03-16:13
 * @Description:获取国际化的设置，判断用哪一国的语言
 */
public class I18nUtil {

    //创建日志对象
    private static Logger logger = LoggerFactory.getLogger(I18nUtil.class);

    //处理配置文件对象
    private static Properties prop = null;

    //寻找对应的配置文件导入区域语言设置
    public static Properties loadI18nProp(){
        if(prop != null){
            return  prop;
        }
        try{
            //从配置信息中得到使用哪个区域
            String i18n = XxlJobAdminConfig.getAdminConfig().getI18n();
            //拼接配置路径
            String i18nFile = MessageFormat.format("i18n/message_{0}.properties", i18n);

            //把国际化配置导入到properties对象中,在这之中进行了utf8编码
            Resource resource = new ClassPathResource(i18nFile);
            EncodedResource encodedResource = new EncodedResource(resource,"UTF-8");
            prop = PropertiesLoaderUtils.loadProperties(encodedResource);

        }catch (IOException e){
            logger.error(e.getMessage(),e);
        }
        return prop;
    }

    /**
     * 根据key从配置类中获取对应的属性值
     */
    public static String getString(String key){
        return loadI18nProp().getProperty(key);
    }


    /**
     * 根据key的值把配置对象转换为json字符串
     */
    public static String getMultString(String... keys){
        Map<String, String> map = new HashMap<String,String>();

        Properties prop = loadI18nProp();
        if(keys != null && keys.length > 0){
            for(String key:keys){
                map.put(key,prop.getProperty(key));
            }
        }else  {
            for(String key: prop.stringPropertyNames()){   //去获取所有的键值对
                map.put(key,prop.getProperty(key));
            }
        }

        String json = JacksonUtil.writeValueAsString(map);
        return json;
    }



}
