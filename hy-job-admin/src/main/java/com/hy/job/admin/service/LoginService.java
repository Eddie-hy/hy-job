package com.hy.job.admin.service;

import com.hy.job.admin.core.model.XxlJobUser;
import com.hy.job.admin.core.util.CookieUtil;
import com.hy.job.admin.core.util.I18nUtil;
import com.hy.job.admin.core.util.JacksonUtil;
import com.hy.job.admin.dao.XxlJobUserDao;
import com.hy.job.core.biz.model.ReturnT;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigInteger;

/**
 * @Author: HY
 * @Date: 2023-10-13-16:34
 * @Description:登录校检类
 */
@Configuration
public class LoginService {

    public static final String LOGIN_IDENTITY_KEY = "XXL_JOB_LOGIN_IDENTITY";

    @Resource
    private XxlJobUserDao xxlJobUserDao;


    //生成身份令牌,也就是把对象转换为16进制的数据
    private String makeToken(XxlJobUser xxlJobUser){
        String tokenJson = JacksonUtil.writeValueAsString(xxlJobUser);
        String tokenHex = new BigInteger(tokenJson.getBytes()).toString(16);

        return tokenHex;
    }

    //读取令牌，还原回对象
    private XxlJobUser parseToken(String tokenHex){
        XxlJobUser xxlJobUser = null;
        if (tokenHex != null) {
            String tokenJson = new String(new BigInteger(tokenHex, 16).toByteArray());      // username_password(md5)
            xxlJobUser = JacksonUtil.readValue(tokenJson, XxlJobUser.class);
        }
        return xxlJobUser;
    }

    /**
     * 登录判断,成功后再把身份信息存进cookie中
     * @param request
     * @param response
     * @param username
     * @param password
     * @param ifRemember
     * @return
     */
    public ReturnT<String> login(HttpServletRequest request, HttpServletResponse response, String username, String password, boolean ifRemember){

        //判断参数是否符合规范
        if(username == null || username.trim().length() == 0 || password == null || password.trim().length() == 0){
            return new ReturnT<String>(500, I18nUtil.getString("login_param_empty"));
        }

        //判断密码是否有效
        XxlJobUser xxlJobUser =xxlJobUserDao.loadByUserName(username);
        if(xxlJobUser == null){
            return new ReturnT<String>(500, I18nUtil.getString("login_param_empty"));
        }
        String passwordMd5 = DigestUtils.md5DigestAsHex(password.getBytes());
        if(!passwordMd5.equals(xxlJobUser.getPassword())){
            return new ReturnT<String>(500, I18nUtil.getString("login_param_empty"));
        }

        String loginToken = makeToken(xxlJobUser);

        //把身份信息存进cookie中
        CookieUtil.set(response, LOGIN_IDENTITY_KEY, loginToken,ifRemember);

        return ReturnT.SUCCESS;
    }




    /**
     * 移除cookie中的用户信息，也就是注销登出
     * @param request
     * @param response
     * @return
     */
    public ReturnT<String> logout(HttpServletRequest request,HttpServletResponse response){
        CookieUtil.remove(request, response, LOGIN_IDENTITY_KEY);
        return ReturnT.SUCCESS;
    }

    //判断用户是否登录
    public XxlJobUser ifLogin(HttpServletRequest request, HttpServletResponse response) {
        String cookieToken = CookieUtil.getValue(request, LOGIN_IDENTITY_KEY);
        if(cookieToken != null){
            XxlJobUser cookieUser = null;

            try{
                cookieUser = parseToken(cookieToken);
            }catch (Exception e){
                 logout(request, response);
            }

            //如果不为空，就加载数据库中关于用户的信息
            if(cookieUser != null){
                XxlJobUser dbUser = xxlJobUserDao.loadByUserName(cookieUser.getUsername());
                if(dbUser != null){
                    if(cookieUser.getPassword().equals(dbUser.getPassword())){
                        return dbUser;
                    }
                }
            }
        }
        return null;
    }
}
