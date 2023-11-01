package com.hy.job.core.handler.impl;

import com.hy.job.core.context.XxlJobHelper;
import com.hy.job.core.handler.IJobHandler;

/**
 * @Author: HY
 * @Date: 2023-10-26-15:33
 * @Description:用于处理glue的处理类
 */
public class GlueJobHandler extends IJobHandler {

    private long glueUpdatetime;

    private IJobHandler jobHandler;

    public GlueJobHandler(IJobHandler jobHandler, long glueUpdatetime){
        this.jobHandler = jobHandler;
        this.glueUpdatetime = glueUpdatetime;
    }

    public long getGlueUpdatetime() {
        return glueUpdatetime;
    }

    @Override
    public void execute() throws Exception {
        XxlJobHelper.log("----------- glue.version:"+ glueUpdatetime +" -----------");
        jobHandler.execute();
    }

    @Override
    public void init() throws Exception {
        this.jobHandler.init();
    }

    @Override
    public void destroy() throws Exception {
        this.jobHandler.destroy();
    }
}
