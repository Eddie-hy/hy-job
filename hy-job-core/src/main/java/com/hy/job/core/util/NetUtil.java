package com.hy.job.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * @Author: HY
 * @Date: 2023-10-21-21:52
 * @Description:网络工具类
 */
public class NetUtil {

    private static Logger logger = LoggerFactory.getLogger(NetUtil.class);

    /**
     * 寻找可用的端口号 (在一定范围内寻找可以端口号，先向上找，再向下找，没找到抛出异常)
     */
    public static int findAvailablePort(int defaultPort){
        int portTmp = defaultPort;
        while (portTmp < 65535){
            if(!isPortUsed(portTmp)){
                return portTmp;
            }else {
                portTmp++;
            }
        }
        portTmp = defaultPort--;
        while (portTmp > 0 ){
            if(!isPortUsed(portTmp)){
                return portTmp;
            }else {
                portTmp--;
            }
        }
        throw new RuntimeException("no available port.");
    }


    /**
     * 检查端口号是否可用  没有被使用返回false 被使用返回true
     */
    public static boolean isPortUsed(int port){
        boolean used = false;
        ServerSocket serverSocket = null;

        try {
            //创建ServerSocket对象绑定端口
            serverSocket = new ServerSocket(port);
            used = false;
        }catch (IOException e){
            logger.info(">>>>>>>>>>> xxl-job, port[{}] is in use.", port);
            used = true;
        }finally {
            if(serverSocket != null){
                try {
                    serverSocket.close();
                }catch (IOException e){
                    logger.info("");
                }
            }
        }
        return used;
    }

}
