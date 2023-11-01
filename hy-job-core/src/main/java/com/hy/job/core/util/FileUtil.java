package com.hy.job.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @Author: HY
 * @Date: 2023-10-21-14:06
 * @Description:文件夹处理类
 */
public class FileUtil {

    private static Logger logger = LoggerFactory.getLogger(FileUtil.class);


    /**
     * 深度优先删除，可以删除指定文件夹下所有的文件夹以及本文件夹
     */
    public static boolean deleteRecursively(File root){
        if(root != null && root.exists()){
            if(root.isDirectory()){
                File[] children = root.listFiles();
                if(children != null){
                    for(File child : children){
                        deleteRecursively(child);
                    }
                }
            }
            return root.delete();
        }
        return false;
    }

    /**
     * 删除文件
     */
    public static void deleteFile(String fileName){
        //文件
        File file = new File(fileName);
        if(file.exists()){
            file.delete();
        }
    }

    /**
     * 将数据保存到文件
     */
    public static void writeFileContent(File file,byte[] data){

        //文件不存在，创建
        if(!file.exists()){
         file.getParentFile().mkdirs();
        }

        //添加文件内容
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(data);
            fos.flush();
        }catch (Exception e){
            logger.error(e.getMessage(),e);
        }finally {
            if(fos != null){
                try {
                    fos.close();
                }catch (IOException e){
                    logger.error(e.getMessage(),e);
                }
            }
        }

    }

    /**
     * 读取文件信息
     */
    public static byte[] readFileContent(File file){
        Long filelength = file.length();
        byte[] filecontent = new byte[filelength.intValue()];

        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            in.read(filecontent);
            in.close();

            return filecontent;
        }catch (Exception e){
            logger.error(e.getMessage(),e);
            return null;
        }finally {
            if(in != null) {
                try {
                    in.close();
                }catch (IOException e){
                    logger.error(e.getMessage(),e);
                }
            }
            }
        }


}
