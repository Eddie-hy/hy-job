package com.hy.job.admin.core.util;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.TemplateHashModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @Author: HY
 * @Date: 2023-10-04-15:29
 * @Description:这个工具类的主要目的是将指定的 Java 包中的静态模型包装成 FreeMarker 模板可以识别的对象。
 * 通常，在 FreeMarker 模板中，你可以使用这个静态模型来调用指定包中的静态方法和属性，从而在模板中实现一些逻辑。
 */
public class FtlUtil {

    private static Logger logger = LoggerFactory.getLogger(FtlUtil.class);

    //创建一个可以把Java 对象包装成 FreeMarker 模板可以识别的对象
    private static BeansWrapper wrapper = new BeansWrapperBuilder(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS).build();

    public static TemplateHashModel generateStaticModel(String packageName){
        try {
            TemplateHashModel staticModels = wrapper.getStaticModels();
            TemplateHashModel fileStatics =(TemplateHashModel) staticModels.get(packageName);
            return fileStatics;
        }catch (Exception e){
            logger.error(e.getMessage(),e);
        }
        return null;
    }

}
