package com.hy.job.admin.core.route.strategy;

import com.hy.job.admin.core.route.ExecutorRouter;
import com.hy.job.core.biz.model.ReturnT;
import com.hy.job.core.biz.model.TriggerParam;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @Author: HY
 * @Date: 2023-10-07-14:24
 * @Description:分组下机器地址相同，不同JOB均匀散列在不同机器上，保证分组下机器分配JOB平均；且每个JOB固定调度其中一台机器；
 *       a、virtual node：解决不均衡问题
 *       b、hash method replace hashCode：String的hashCode可能重复，需要进一步扩大hashCode的取值范围
 */
public class ExecutorRouteConsistentHash extends ExecutorRouter {

    private static int VIRTUAL_NODE_NUM = 100;

    /**
     * 这个 hash 方法将输入的字符串 key 转换为一个 32 位的哈希值，以便后续的一致性哈希计算。一致性哈希通常需要使用一个哈希函数来确定任务应该路由到哪个执行机器。
     * get hash code on 2^32 ring (md5散列的方式计算hash值)
     * @param key
     * @return
     */
    private static long hash(String key) {

        // md5 byte
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
        md5.reset();
        byte[] keyBytes = null;
        try {
            keyBytes = key.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unknown string :" + key, e);
        }

        md5.update(keyBytes);
        byte[] digest = md5.digest();

        // hash code, Truncate to 32-bits
        long hashCode = ((long) (digest[3] & 0xFF) << 24)
                | ((long) (digest[2] & 0xFF) << 16)
                | ((long) (digest[1] & 0xFF) << 8)
                | (digest[0] & 0xFF);

        long truncateHashCode = hashCode & 0xffffffffL;
        return truncateHashCode;
    }

    /**
     * hashJob 方法的目标是根据给定的 jobId 和地址列表，在一致性哈希环上选择一个执行机器，以实现负载均衡和任务分发
     * @param jobId
     * @param addressList
     * @return
     */
    public String hashJob(int jobId , List<String> addressList){
        TreeMap<Long, String> addressRing = new TreeMap<Long,String>();
        for (String address : addressList){
            for(int i = 0;i < VIRTUAL_NODE_NUM;i++){
                long addressHash = hash("SHARD-" + address + "-NODE-" + i);
                //把哈希值和地址映射进map
                addressRing.put(addressHash, address);
            }
        }
        long jobHash = hash(String.valueOf(jobId));
        //返回的是 addressRing 中键大于等于 jobHash 的部分
        SortedMap<Long , String> lastRing = addressRing.tailMap(jobHash);
        if(!lastRing.isEmpty()){
            return lastRing.get(lastRing.firstKey());
        }
        //返回最小的键值对
        return addressRing.firstEntry().getValue();
    }



    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        String address = hashJob(triggerParam.getJobId(), addressList);
        return new ReturnT<String>(address);
    }
}
