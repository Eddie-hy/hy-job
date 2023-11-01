package com.hy.job.admin.core.route.strategy;

import com.hy.job.admin.core.route.ExecutorRouter;
import com.hy.job.core.biz.model.ReturnT;
import com.hy.job.core.biz.model.TriggerParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Author: HY
 * @Date: 2023-10-07-16:19
 * @Description:
 */
public class ExecutorRouteLRU extends ExecutorRouter {

    private static ConcurrentMap<Integer, LinkedHashMap<String, String>> jobLRUMap = new ConcurrentHashMap<Integer, LinkedHashMap<String, String>>();

    private static long CACHE_VALID_TIME = 0;

    public String route(int jobId , List<String> addressList){

        //清除缓存
        if(System.currentTimeMillis() > CACHE_VALID_TIME){
            jobLRUMap.clear();
            CACHE_VALID_TIME = System.currentTimeMillis() + 1000*60*60*24;
        }

        //初始化lru存储
        LinkedHashMap<String , String> lruItem = jobLRUMap.get(jobId);
        if(lruItem == null){
            /**
             * LinkedHashMap(最近范围的元素会在尾部)
             *      a、accessOrder：true=访问顺序排序（get/put时排序）；false=插入顺序排期；
             *      b、removeEldestEntry：新增元素时将会调用，返回true时会删除最老元素；可封装LinkedHashMap并重写该方法，比如定义最大容量，超出是返回true即可实现固定长度的LRU算法；
             */
            lruItem = new LinkedHashMap<String, String>(16, 0.75f, true);
            jobLRUMap.putIfAbsent(jobId, lruItem);
        }

        //存放新的地址如果有的话
        for(String address : addressList){
            if(!lruItem.containsKey(address)){
                lruItem.put(address, address);
            }
        }

        //移除旧的值
        List<String> delKeys = new ArrayList<>();
        for(String existKey :lruItem.keySet()){
            if(!addressList.contains(existKey)){
                delKeys.add(existKey);
            }
        }

        if(delKeys.size() > 0){
            for(String delKey : delKeys){
                lruItem.remove(delKey);
            }
        }

        //把地址传回去
        //获取第一个元素，也就是最老的最少使用的元素
        String eldestKey = lruItem.entrySet().iterator().next().getKey();
        String eldestValue = lruItem.get(eldestKey);
        return eldestValue;
    }

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        String address = route(triggerParam.getJobId(),addressList);
        return new ReturnT<String>(address);
    }
}
