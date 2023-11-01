package com.hy.job.admin.service.impl;

import com.hy.job.admin.core.thread.JobCompleteHelper;
import com.hy.job.admin.core.thread.JobRegistryHelper;
import com.hy.job.core.biz.AdminBiz;
import com.hy.job.core.biz.model.HandleCallbackParam;
import com.hy.job.core.biz.model.RegistryParam;
import com.hy.job.core.biz.model.ReturnT;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: HY
 * @Date: 2023-10-13-16:34
 * @Description:
 */
@Service
public class AdminBizImpl implements AdminBiz {
    @Override
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {
        return JobCompleteHelper.getInstance().callback(callbackParamList);
    }

    @Override
    public ReturnT<String> registry(RegistryParam registryParam) {
        return JobRegistryHelper.getInstance().registry(registryParam);
    }

    @Override
    public ReturnT<String> registryRemove(RegistryParam registryParam) {
        return JobRegistryHelper.getInstance().registryRemove(registryParam);
    }
}
