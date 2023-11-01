package com.hy.job.admin.core.util;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class JacksonUtil {

    //创建日志对象
    private static Logger logger = LoggerFactory.getLogger(JacksonUtil.class);

    //创建序列化对象
    private final static ObjectMapper objectMapper = new ObjectMapper();

    //封装对象的函数
    public static ObjectMapper getInstance(){
        return objectMapper;
    }


    /**
     * 用来把java对象转换为json字符串
     * @param obj
     * @return
     */
    public static String writeValueAsString(Object obj) {
        try {
            return getInstance().writeValueAsString(obj);
        } catch (JsonGenerationException e) {
            logger.error(e.getMessage(), e);
        } catch (JsonMappingException e) {
            logger.error(e.getMessage(), e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }


    /**
     * 用来把json字符串转换为java对象的函数
     * @param jsonStr
     * @param clazz
     * @return
     * @param <T>
     */
    public static <T> T readValue(String jsonStr, Class<T> clazz) {
        try {
            return getInstance().readValue(jsonStr, clazz);
        } catch (JsonParseException e) {
            logger.error(e.getMessage(), e);
        } catch (JsonMappingException e) {
            logger.error(e.getMessage(), e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }


    /**
     * 把json字符串转换为java对象，可以处理具有泛型信息的json数据也可以被识别，适应不同类型的泛型数据解析需求
     * @param jsonStr
     * @param parametrized
     * @param parameterClasses
     * @return
     * @param <T>
     */
    public static <T> T readValue(String jsonStr, Class<?> parametrized, Class<?>... parameterClasses) {
        try {
            JavaType javaType = getInstance().getTypeFactory().constructParametricType(parametrized, parameterClasses);
            return getInstance().readValue(jsonStr, javaType);
        } catch (JsonParseException e) {
            logger.error(e.getMessage(), e);
        } catch (JsonMappingException e) {
            logger.error(e.getMessage(), e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }


}
