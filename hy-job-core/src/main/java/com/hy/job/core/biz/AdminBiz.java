package com.hy.job.core.biz;

import com.hy.job.core.biz.model.HandleCallbackParam;
import com.hy.job.core.biz.model.RegistryParam;
import com.hy.job.core.biz.model.ReturnT;

import java.util.List;

/**
 * @Author: HY
 * @Date: 2023-10-12-21:14
 * @Description:客户端接口
 */
public interface AdminBiz {

    // ---------------------- callback ----------------------

    /**
     * callback
     *
     * @param callbackParamList
     * @return
     */
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList);


    // ---------------------- registry ----------------------

    /**
     * registry
     *
     * @param registryParam
     * @return
     */
    public ReturnT<String> registry(RegistryParam registryParam);

    /**
     * registry remove
     *
     * @param registryParam
     * @return
     */
    public ReturnT<String> registryRemove(RegistryParam registryParam);


    // ---------------------- biz (custome) ----------------------
    // group、job ... manage
}
