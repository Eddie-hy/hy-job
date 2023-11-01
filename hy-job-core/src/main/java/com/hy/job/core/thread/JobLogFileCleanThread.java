package com.hy.job.core.thread;

import com.hy.job.core.log.XxlJobFileAppender;
import com.hy.job.core.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @Author: HY
 * @Date: 2023-10-21-13:10
 * @Description:日志清理操作
 */
public class JobLogFileCleanThread {

    private static Logger logger = LoggerFactory.getLogger(JobLogFileCleanThread.class);

    private static JobLogFileCleanThread instance = new JobLogFileCleanThread();

    public static JobLogFileCleanThread getInstance() {return instance;}

    private Thread localThread;

    private volatile  boolean toStop = false;

    public void start(final long logRetentionDays){

        //限制日志清理最小时间
        if(logRetentionDays < 3){
            return;
        }

        localThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!toStop){
                    try{
                        //清理大于最大保存时间的日志文件
                        File[] childDirs = new File(XxlJobFileAppender.getLogPath()).listFiles();
                        if(childDirs != null && childDirs.length > 0){

                            //今天的时间
                            Calendar todayCal = Calendar.getInstance();
                            todayCal.set(Calendar.HOUR_OF_DAY,0);
                            todayCal.set(Calendar.MINUTE,0);
                            todayCal.set(Calendar.SECOND,0);
                            todayCal.set(Calendar.MILLISECOND,0);

                            Date todayDate = todayCal.getTime();

                            for(File childFile: childDirs){

                                //判断文件是否是一个目录
                                if(!childFile.isDirectory()){
                                    continue;
                                }

                                //判断是否命名规范
                                if(childFile.getName().indexOf("-") == -1){
                                    continue;
                                }

                                //判断文件创建时间
                                Date logFileCreateDate = null;
                                try {
                                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                                    logFileCreateDate = simpleDateFormat.parse(childFile.getName());
                                }catch (Exception e){
                                    logger.error(e.getMessage(),e);
                                }
                                if(logFileCreateDate == null){
                                    continue;
                                }

                                //删除过期文件夹
                                if ((todayDate.getTime()-logFileCreateDate.getTime()) >= logRetentionDays * (24 * 60 * 60 * 1000) ) {
                                    FileUtil.deleteRecursively(childFile);
                                }
                            }
                        }
                    }catch (Exception e){
                        if(!toStop){
                            logger.error(e.getMessage(),e);
                        }
                    }

                    //删除完这天的日志，睡眠一天再删除
                    try {
                        TimeUnit.DAYS.sleep(1);
                    }catch (InterruptedException e){
                        if(!toStop){
                            logger.error(e.getMessage(),e);
                        }
                    }
                }
                logger.info(">>>>>>>>>>> xxl-job, executor JobLogFileCleanThread thread destroy.");
            }
        });
        localThread.setDaemon(true);
        localThread.setName("xxl-job, executor JobLogFileCleanThread");
        localThread.start();
    }

    public void toStop(){
        toStop = true;

        if(localThread == null){
            return;
        }

        //中断然后等待运行完成
        localThread.interrupt();
        try {
            localThread.join();
        }catch (InterruptedException e){
            logger.error(e.getMessage(),e);
        }
    }

}
