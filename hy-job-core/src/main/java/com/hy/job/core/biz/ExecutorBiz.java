package com.hy.job.core.biz;

import com.hy.job.core.biz.model.*;

/**
 * @Author: HY
 * @Date: 2023-10-06-11:24
 * @Description:执行器接口
 */
public interface ExecutorBiz {

    /**
     * beat
     * @return
     */
    ReturnT<String> beat();

    /**
     * idle beat
     *
     * @param idleBeatParam
     * @return
     */
    public ReturnT<String> idleBeat(IdleBeatParam idleBeatParam);

    /**
     * run
     * @param triggerParam
     * @return
     */
    public ReturnT<String> run(TriggerParam triggerParam);


    /**
     * kill
     * @param killParam
     * @return
     */
    ReturnT<String> kill(KillParam killParam);

    /**
     * log
     * @param logParam
     * @return
     */
    public ReturnT<LogResult> log(LogParam logParam);

}
