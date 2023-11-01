package com.hy.job.core.thread;

import com.hy.job.core.biz.AdminBiz;
import com.hy.job.core.biz.model.RegistryParam;
import com.hy.job.core.biz.model.ReturnT;
import com.hy.job.core.enums.RegistryConfig;
import com.hy.job.core.executor.XxlJobExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @Author: HY
 * @Date: 2023-10-22-15:11
 * @Description:注册中心启动与停止
 */
public class ExecutorRegistryThread {

    private static Logger logger = LoggerFactory.getLogger(ExecutorRegistryThread.class);

    private static ExecutorRegistryThread instance = new ExecutorRegistryThread();

    public static ExecutorRegistryThread getInstance(){
        return instance;
    }

    private Thread registryThread;

    private volatile  boolean toStop = false;

    public void start(final String appname , final String address){

        //判断参数是否有效
        if(appname == null || appname.trim().length() == 0){
            logger.warn(">>>>>>>>>>> xxl-job, executor registry config fail, appname is null.");
            return;
        }
        if (XxlJobExecutor.getAdminBizList() == null) {
            logger.warn(">>>>>>>>>>> xxl-job, executor registry config fail, adminAddresses is null.");
            return;
        }

        registryThread = new Thread(new Runnable() {

            @Override
            public void run() {

                //注册
                while (!toStop){
                    try {
                        RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), appname, address);
                        for(AdminBiz adminBiz: XxlJobExecutor.getAdminBizList()){
                            try {
                                ReturnT<String> registryResult = adminBiz.registry(registryParam);
                                if(registryResult != null && ReturnT.SUCCESS_CODE == registryResult.getCode()){
                                    registryResult = ReturnT.SUCCESS;
                                    logger.debug(">>>>>>>>>>> xxl-job registry success, registryParam:{}, registryResult:{}", new Object[]{registryParam, registryResult});
                                    break;
                                }else {
                                    logger.info(">>>>>>>>>>> xxl-job registry fail, registryParam:{}, registryResult:{}", new Object[]{registryParam, registryResult});
                                }
                            }catch (Exception e){
                                logger.info(">>>>>>>>>>> xxl-job registry error, registryParam:{}", registryParam, e);
                            }
                        }
                    }catch (Exception e){
                        if(!toStop){
                            logger.error(e.getMessage(), e);
                        }
                    }
                    try {
                        if (!toStop) {
                            TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                        }
                    } catch (InterruptedException e) {
                        if (!toStop) {
                            logger.warn(">>>>>>>>>>> xxl-job, executor registry thread interrupted, error msg:{}", e.getMessage());
                        }
                    }
                }

                //注册移除
                try {
                    RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), appname, address);
                    for (AdminBiz adminBiz: XxlJobExecutor.getAdminBizList()) {
                        try {
                            ReturnT<String> registryResult = adminBiz.registryRemove(registryParam);
                            if (registryResult!=null && ReturnT.SUCCESS_CODE == registryResult.getCode()) {
                                registryResult = ReturnT.SUCCESS;
                                logger.info(">>>>>>>>>>> xxl-job registry-remove success, registryParam:{}, registryResult:{}", new Object[]{registryParam, registryResult});
                                break;
                            } else {
                                logger.info(">>>>>>>>>>> xxl-job registry-remove fail, registryParam:{}, registryResult:{}", new Object[]{registryParam, registryResult});
                            }
                        } catch (Exception e) {
                            if (!toStop) {
                                logger.info(">>>>>>>>>>> xxl-job registry-remove error, registryParam:{}", registryParam, e);
                            }

                        }

                    }
                }catch (Exception e){
                    if(!toStop){
                        logger.error(e.getMessage() , e);
                    }
                }
                logger.info(">>>>>>>>>>> xxl-job, executor registry thread destroy.");

            }
        });
        registryThread.setDaemon(true);
        registryThread.setName("xxl-job, executor ExecutorRegistryThread");
        registryThread.start();
    }

    public void toStop(){
        toStop = true;

        //中断然后等待
        if(registryThread !=null) {
            registryThread.interrupt();
            try {
               registryThread.join();
            }catch (InterruptedException e){
                logger.error(e.getMessage() , e);
            }
        }
        }

}
