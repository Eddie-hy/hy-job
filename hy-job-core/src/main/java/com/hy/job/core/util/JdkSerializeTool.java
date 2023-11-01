package com.hy.job.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * @Author: HY
 * @Date: 2023-10-20-16:57
 * @Description:进行对象的序列化和反序列化，将对象转换成字节数组并将字节数组还原为对象。主要用于将对象转化为字节数组以便在各种场景中进行传输或持久化。
 */
public class JdkSerializeTool {

    private static Logger logger = LoggerFactory.getLogger(JdkSerializeTool.class);

    // ------------------------ serialize and unserialize ------------------------

    /**
     * 将对象-->byte[] (由于jedis中不支持直接存储object所以转换成byte[]存入)
     *
     * @param object
     * @return
     */
    public static byte[] serialize(Object object) {
        ObjectOutputStream oos = null;
        ByteArrayOutputStream baos = null;

        try {
            //序列化
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            byte[] bytes = baos.toByteArray();
            return bytes;
        }catch (Exception e){
            logger.error(e.getMessage(),e);
        }finally {
            try {
                oos.close();
                baos.close();
            }catch (IOException e){
                logger.error(e.getMessage(),e);
            }
        }
        return null;
    }

    /**
     * 将byte[] -->Object
     *
     * @param bytes
     * @return
     */
    public static <T> Object deserialize(byte[] bytes, Class<T> clazz){
        ByteArrayInputStream bais = null;
        try {
            //反序列化
            bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return ois.readObject();
        }catch (Exception e){
            logger.error(e.getMessage(),e);
        }finally {
            try {
                bais.close();
            }catch (Exception e){
                logger.error(e.getMessage(),e);
            }
        }
        return null;
    }
}