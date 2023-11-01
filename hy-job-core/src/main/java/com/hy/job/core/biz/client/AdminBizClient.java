package com.hy.job.core.biz.client;

import com.hy.job.core.biz.AdminBiz;
import com.hy.job.core.biz.model.HandleCallbackParam;
import com.hy.job.core.biz.model.RegistryParam;
import com.hy.job.core.biz.model.ReturnT;
import com.hy.job.core.util.XxlJobRemotingUtil;

import java.util.List;

/**
 * @Author: HY
 * @Date: 2023-10-13-16:39
 * @Description:用于传输发送反馈信息
 */
public class AdminBizClient implements AdminBiz {

    public AdminBizClient(){

    }

    public AdminBizClient(String addressUrl , String accessToken){
        this.addressUrl = addressUrl;
        this.accessToken  = accessToken;

        //判断连接格式是否有效
        if(!this.addressUrl.endsWith("/")){
            this.addressUrl = this.addressUrl + "/";
        }
    }

    private String addressUrl ;
    private String accessToken;
    private int timeout = 3;
    @Override
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {
        return XxlJobRemotingUtil.postBody(addressUrl+"api/callback", accessToken, timeout, callbackParamList, String.class);
    }

    @Override
    public ReturnT<String> registry(RegistryParam registryParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + "api/registry", accessToken, timeout, registryParam, String.class);
    }

    @Override
    public ReturnT<String> registryRemove(RegistryParam registryParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + "api/registryRemove", accessToken, timeout, registryParam, String.class);
    }
}
