package com.hy.job.core.glue;

import com.hy.job.core.glue.impl.SpringGlueFactory;
import com.hy.job.core.handler.IJobHandler;
import groovy.lang.GroovyClassLoader;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Author: HY
 * @Date: 2023-10-24-10:47
 * @Description:通过名字制造实体类或者实体
 */
public class GlueFactory {

    private static GlueFactory glueFactory = new GlueFactory();

    public static GlueFactory getInstance(){
        return glueFactory;
    }

    public static void refreshInstance(int type){
        if(type == 0){
            glueFactory = new GlueFactory();
        }else if(type == 1){
            glueFactory = new SpringGlueFactory();
        }
    }

    /**
     * 加载groovy类型
     */
    private GroovyClassLoader groovyClassLoader = new GroovyClassLoader();

    private ConcurrentMap<String , Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();

    /**
     * 这个方法的主要作用是动态加载 Groovy 代码，编译为类，然后返回该类的引用，以便在后续代码中使用。这对于在运行时执行动态生成的 Groovy 脚本非常有用。
     * @param codeSource
     * @return
     * @throws Exception
     */
    public IJobHandler loadNewInstance(String codeSource)throws Exception{
        if(codeSource != null && codeSource.trim().length() > 0){
            Class<?> clazz = getCodeSourceClass(codeSource);
            if(clazz != null){
                Object instance = clazz.newInstance();
                if(instance != null){
                    if(instance instanceof  IJobHandler){
                        this.injectService(instance);
                        return (IJobHandler) instance;
                    }else {
                        throw new IllegalArgumentException(">>>>>>>>>>> xxl-glue, loadNewInstance error, "
                                + "cannot convert from instance["+ instance.getClass() +"] to IJobHandler");
                    }
                }
            }
        }
        throw new IllegalArgumentException(">>>>>>>>>>> xxl-glue, loadNewInstance error, instance is null");
    }


    /**
     * 这个方法的主要目的是为了避免重复解析和加载相同的Groovy脚本，提高执行效率，并降低资源消耗。如果已经解析和加载了特定Groovy脚本，它将从缓存中获取该类，而不是重新加载。
     */
    private Class<?> getCodeSourceClass(String codeSource){
        try {
            // md5
            byte[] md5 = MessageDigest.getInstance("MD5").digest(codeSource.getBytes());
            String md5Str = new BigInteger(1, md5).toString(16);

            Class<?> clazz = CLASS_CACHE.get(md5Str);
            if(clazz == null){
                clazz = groovyClassLoader.parseClass(codeSource);
                CLASS_CACHE.putIfAbsent(md5Str, clazz);
            }
            return clazz;
        } catch (Exception e) {
            return groovyClassLoader.parseClass(codeSource);
        }
}


    /**
     * inject service of bean field
     *
     * @param instance
     */
    public void injectService(Object instance){
        //do something
    }
}
