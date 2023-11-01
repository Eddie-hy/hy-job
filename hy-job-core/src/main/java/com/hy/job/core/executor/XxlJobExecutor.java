package com.hy.job.core.executor;

import com.hy.job.core.biz.AdminBiz;
import com.hy.job.core.biz.client.AdminBizClient;
import com.hy.job.core.handler.IJobHandler;
import com.hy.job.core.handler.annotation.XxlJob;
import com.hy.job.core.handler.impl.MethodJobHandler;
import com.hy.job.core.log.XxlJobFileAppender;
import com.hy.job.core.server.EmbedServer;
import com.hy.job.core.thread.JobLogFileCleanThread;
import com.hy.job.core.thread.JobThread;
import com.hy.job.core.thread.TriggerCallbackThread;
import com.hy.job.core.util.IpUtil;
import com.hy.job.core.util.NetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Author: HY
 * @Date: 2023-10-07-20:56
 * @Description:执行器的父类
 */
public class XxlJobExecutor {

    private static final Logger logger = LoggerFactory.getLogger(XxlJobExecutor.class);

    // ---------------------- param ----------------------
    private String adminAddresses;
    private String accessToken;
    private String appname;
    private String address;
    private String ip;
    private int port;
    private String logPath;
    private int logRetentionDays;

    public void setAdminAddresses(String adminAddresses) {
        this.adminAddresses = adminAddresses;
    }
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    public void setAppname(String appname) {
        this.appname = appname;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public void setIp(String ip) {
        this.ip = ip;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }
    public void setLogRetentionDays(int logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
    }

    // ---------------------- start + stop ----------------------
    public void start() throws Exception{

        //初始化日志路径
        XxlJobFileAppender.initLogPath(logPath);

        //初始化激活 管理-客户端
        initAdminBizList(adminAddresses, accessToken);

        //清除过期日志
        JobLogFileCleanThread.getInstance().start(logRetentionDays);

        //初始化触发反馈线程
        TriggerCallbackThread.getInstance().start();

        //初始化嵌入式服务器
        initEmbedServer(address, ip, port, appname, accessToken);

    }

    public void destroy(){
        //销毁嵌入式服务器
        stopEmbedServer();

        //销毁 jobThreadRepository
        if(jobThreadRepository.size() > 0){
            for(Map.Entry<Integer, JobThread> item: jobThreadRepository.entrySet()){
                JobThread oldJobThread = removeJobThread(item.getKey(), "web container destroy and kill the job.");
                //等待工作线程反馈给回调队列
                if(oldJobThread != null) {
                    try {
                        oldJobThread.join();
                    }catch (InterruptedException e){
                        logger.error(">>>>>>>>>>> xxl-job, JobThread destroy(join) error, jobId:{}", item.getKey(), e);
                    }
                }
                }
            jobThreadRepository.clear();
            }
        jobHandlerRepository.clear();

        //销毁工作文件清理线程
        JobLogFileCleanThread.getInstance().toStop();

        //销毁触发反馈线程
        TriggerCallbackThread.getInstance().toStop();

    }


    // ---------------------- admin-client (rpc invoker) ----------------------
    private static List<AdminBiz> adminBizList;

    private void initAdminBizList(String adminAddresses,String accessToken)throws Exception{
        if(adminAddresses != null && adminAddresses.trim().length() > 0){
            for(String address: adminAddresses.trim().split(",")){
                if(address != null && address.trim().length() > 0){

                    AdminBiz adminBiz = new AdminBizClient(address.trim(), accessToken);

                    if(adminBizList == null){
                        adminBizList = new ArrayList<AdminBiz>();
                    }
                    adminBizList.add(adminBiz);
                }
            }
        }
    }

    public static List<AdminBiz> getAdminBizList(){
        return adminBizList;
    }

    // ---------------------- executor-server (rpc provider) ----------------------
    private EmbedServer embedServer = null;

    /**
     * 初始化嵌入式服务器
     * @param address
     * @param ip
     * @param port
     * @param appname
     * @param accessToken
     * @throws Exception
     */
    private void initEmbedServer(String address, String ip, int port, String appname, String accessToken) throws Exception {

        //找到端口号
        port = port>0?port: NetUtil.findAvailablePort(9999);
        ip = (ip != null && ip.trim().length() >0)?ip: IpUtil.getIp();

        //生成地址
        if(address == null || address.trim().length() == 0){
            String ip_port_address = IpUtil.getIpPort(ip, port);
            address = "http://{ip_port}/".replace("{ip_port}", ip_port_address);
        }

        //许可Token
        if(accessToken == null || accessToken.trim().length() == 0){
            logger.warn(">>>>>>>>>>> xxl-job accessToken is empty. To ensure system security, please set the accessToken.");
        }

        //开启嵌入式服务器
        embedServer = new EmbedServer();
        embedServer.start(address, port, appname, accessToken);
    }

    private void stopEmbedServer(){
        //停止嵌入式服务器
        if(embedServer != null){
            try {
                embedServer.stop();
            }catch (Exception e){
                logger.error(e.getMessage(), e);
            }
        }
    }

    // ---------------------- job handler repository ----------------------
    //处理器方法
    private static ConcurrentMap<String, IJobHandler> jobHandlerRepository = new ConcurrentHashMap<String , IJobHandler>();

    public static IJobHandler loadJobHandler(String name){return jobHandlerRepository.get(name);}

    public static IJobHandler registJobHandler(String name , IJobHandler jobHandler){
        logger.info(">>>>>>>>>>> xxl-job register jobhandler success, name:{}, jobHandler:{}", name, jobHandler);
        return jobHandlerRepository.put(name, jobHandler);
    }

    protected void registJobHandler(XxlJob xxlJob, Object bean, Method executeMethod){
        if(xxlJob ==null){
            return;
        }

        //获取xxl-job注解中的值
        String name = xxlJob.value();
        //获取对象的类型信息并执行与该类型相关的操作
        Class<?> clazz = bean.getClass();
        String methodName = executeMethod.getName();
        if (name.trim().length() == 0) {
            throw new RuntimeException("xxl-job method-jobhandler name invalid, for[" + clazz + "#" + methodName + "] .");
        }

        //存在同名的任务处理器，抛出异常
        if(loadJobHandler(name) != null){
            throw new RuntimeException("xxl-job jobhandler[" + name + "] naming conflicts.");
        }

        //绕过 Java 的访问控制
        executeMethod.setAccessible(true);

        //初始化和销毁方法
        Method initMethod = null;
        Method destroyMethod = null;

        if(xxlJob.init().trim().length() > 0){
            try {
                initMethod = clazz.getDeclaredMethod(xxlJob.init());
                initMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("xxl-job method-jobhandler initMethod invalid, for[" + clazz + "#" + methodName + "] .");
            }
        }
        if (xxlJob.destroy().trim().length() > 0) {
            try {
                destroyMethod = clazz.getDeclaredMethod(xxlJob.destroy());
                destroyMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("xxl-job method-jobhandler destroyMethod invalid, for[" + clazz + "#" + methodName + "] .");
            }
        }

        //注册处理方法
        registJobHandler(name, new MethodJobHandler(bean, executeMethod, initMethod, destroyMethod));

    }



    // ---------------------- job thread repository ----------------------
    //线程仓库
    private static ConcurrentMap<Integer, JobThread> jobThreadRepository = new ConcurrentHashMap<Integer, JobThread>();

    /**
     * 将新任务加入map中并执行，同时替换出旧任务并停止
     * @param jobId
     * @param handler
     * @param removeOldReason
     * @return
     */
    public static JobThread registJobThread(int jobId, IJobHandler handler, String removeOldReason){
        JobThread newJobThread = new JobThread(jobId, handler);
        newJobThread.start();
        logger.info(">>>>>>>>>>> xxl-job regist JobThread success, jobId:{}, handler:{}", new Object[]{jobId, handler});

        JobThread oldJobThread = jobThreadRepository.put(jobId, newJobThread);	// putIfAbsent | oh my god, map's put method return the old value!!!
        if(oldJobThread != null){
            oldJobThread.toStop(removeOldReason);
            oldJobThread.interrupt();
        }
        return newJobThread;
    }

    public static JobThread removeJobThread(int jobId, String removeOldReason){
        JobThread oldJobThread = jobThreadRepository.remove(jobId);
        if(oldJobThread != null){
            oldJobThread.toStop(removeOldReason);
            oldJobThread.interrupt();

            return oldJobThread;
        }
        return null;
    }


    public static JobThread loadJobThread(int jobId){
        return jobThreadRepository.get(jobId);
    }
}
