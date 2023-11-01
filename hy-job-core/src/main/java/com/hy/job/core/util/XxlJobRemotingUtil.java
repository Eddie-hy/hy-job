package com.hy.job.core.util;

import com.hy.job.core.biz.model.ReturnT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * @Author: HY
 * @Date: 2023-10-12-21:32
 * @Description:这个工具类用于与XXL-Job调度中心进行远程通信，例如向调度中心注册执行器、回调执行结果等
 */
public class XxlJobRemotingUtil {
    private static Logger logger = LoggerFactory.getLogger(XxlJobRemotingUtil.class);
    public static final String XXL_JOB_ACCESS_TOKEN = "XXL-JOB-ACCESS-TOKEN";

    /**
     * 允许与不受信任的 SSL 证书的主机建立安全连接，而不进行 SSL 证书验证
     */
    private static void trustAllHosts(HttpsURLConnection connection) {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            SSLSocketFactory newFactory = sc.getSocketFactory();

            connection.setSSLSocketFactory(newFactory);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        connection.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
    }

    /**
     * 实现信任所有的 SSL 证书，无论其颁发者或服务器的身份
     */
    private static final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[]{};
        }
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }
    }};




    /**
     * 用于与某个远程服务进行通信，发送请求并接收响应，通常用于调用远程接口或微服务
     */
    public static ReturnT postBody(String url, String accessToken, int timeout, Object requestObj, Class returnTargClassOfT) {

        //创建url对象
        HttpURLConnection connection = null;
        BufferedReader bufferedReader = null;

        try {
            //建立连接
            URL realUrl = new URL(url);
            connection = (HttpURLConnection) realUrl.openConnection();

            //信任https链接
            boolean useHttps = url.startsWith("https");
            if(useHttps){
                HttpsURLConnection https = (HttpsURLConnection) connection;
                trustAllHosts(https);
            }

            //链接限制设置
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setReadTimeout(timeout * 1000);
            connection.setConnectTimeout(3 * 1000);
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("Accept-Charset", "application/json;charset=UTF-8");

            if(accessToken != null && accessToken.trim().length() > 0){
                connection.setRequestProperty(XXL_JOB_ACCESS_TOKEN, accessToken);
            }

            //执行连接
            connection.connect();

            //把请求体写入参数
            if(requestObj != null){
                String requestBody = GsonTool.toJson(requestObj);

                //把数据写入https请求中
                DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
                dataOutputStream.write(requestBody.getBytes("UTF-8"));
                dataOutputStream.flush();
                dataOutputStream.close();
            }

            //判断状态是否有效
            int statusCode = connection.getResponseCode();
            if(statusCode != 200){
                return new ReturnT<String>(ReturnT.FAIL_CODE, "xxl-job remoting fail, StatusCode("+ statusCode +") invalid. for url : " + url);
            }

            //结果
            bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }
            String resultJson = result.toString();

            //转换返回结果的格式
            try {
                ReturnT returnT = GsonTool.fromJson(resultJson, ReturnT.class, returnTargClassOfT);
                return returnT;
            } catch (Exception e) {
                logger.error("xxl-job remoting (url="+url+") response content invalid("+ resultJson +").", e);
                return new ReturnT<String>(ReturnT.FAIL_CODE, "xxl-job remoting (url="+url+") response content invalid("+ resultJson +").");
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ReturnT<String>(ReturnT.FAIL_CODE, "xxl-job remoting error("+ e.getMessage() +"), for url : " + url);
        }finally {
            //收尾工作
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception e2) {
                logger.error(e2.getMessage(), e2);
            }
        }
    }


}
