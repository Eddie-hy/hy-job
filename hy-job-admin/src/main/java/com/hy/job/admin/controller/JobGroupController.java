package com.hy.job.admin.controller;

import com.hy.job.admin.controller.annotation.PermissionLimit;
import com.hy.job.admin.core.model.XxlJobGroup;
import com.hy.job.admin.core.model.XxlJobRegistry;
import com.hy.job.admin.core.util.I18nUtil;
import com.hy.job.admin.dao.XxlJobGroupDao;
import com.hy.job.admin.dao.XxlJobInfoDao;
import com.hy.job.admin.dao.XxlJobRegistryDao;
import com.hy.job.core.biz.model.ReturnT;
import com.hy.job.core.enums.RegistryConfig;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @Author: HY
 * @Date: 2023-10-06-17:09
 * @Description:执行器管理
 */
@Controller
@RequestMapping("/jobgroup")
public class JobGroupController {

    @Resource
    public XxlJobInfoDao xxlJobInfoDao;
    @Resource
    public XxlJobGroupDao xxlJobGroupDao;
    @Resource
    private XxlJobRegistryDao xxlJobRegistryDao;


    @RequestMapping
    @PermissionLimit(adminuser = true)
    public String index(Model model){
        return "jobgroup/jobgroup.index";
    }

    @RequestMapping("/pageList")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public Map<String, Object> pageList(HttpServletRequest request,
                                        @RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        String appname, String title) {


        //查询页面
        List<XxlJobGroup> list = xxlJobGroupDao.pageList(start, length, appname, title);
        int list_count = xxlJobGroupDao.pageListCount(start, length, appname, title);

        //封装数据
        Map<String, Object> maps = new HashMap<String, Object>();
        maps.put("recordsTotal", list_count);		// 总记录数
        maps.put("recordsFiltered", list_count);	// 过滤后的总记录数
        maps.put("data", list);  					// 分页列表

        return maps;
    }


    @RequestMapping("/save")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<String> save(XxlJobGroup xxlJobGroup){

        //判断执行器命名是否有效
        if (xxlJobGroup.getAppname()==null || xxlJobGroup.getAppname().trim().length()==0) {
            return new ReturnT<String>(500, (I18nUtil.getString("system_please_input")+"AppName") );
        }
        if (xxlJobGroup.getAppname().length()<4 || xxlJobGroup.getAppname().length()>64) {
            return new ReturnT<String>(500, I18nUtil.getString("jobgroup_field_appname_length") );
        }
        if (xxlJobGroup.getAppname().contains(">") || xxlJobGroup.getAppname().contains("<")) {
            return new ReturnT<String>(500, "AppName"+I18nUtil.getString("system_unvalid") );
        }
        if (xxlJobGroup.getTitle()==null || xxlJobGroup.getTitle().trim().length()==0) {
            return new ReturnT<String>(500, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobgroup_field_title")) );
        }
        if (xxlJobGroup.getTitle().contains(">") || xxlJobGroup.getTitle().contains("<")) {
            return new ReturnT<String>(500, I18nUtil.getString("jobgroup_field_title")+I18nUtil.getString("system_unvalid") );
        }
        if (xxlJobGroup.getAddressType()!=0) {
            if (xxlJobGroup.getAddressList()==null || xxlJobGroup.getAddressList().trim().length()==0) {
                return new ReturnT<String>(500, I18nUtil.getString("jobgroup_field_addressType_limit") );
            }
            if (xxlJobGroup.getAddressList().contains(">") || xxlJobGroup.getAddressList().contains("<")) {
                return new ReturnT<String>(500, I18nUtil.getString("jobgroup_field_registryList")+I18nUtil.getString("system_unvalid") );
            }

            String[] addresss = xxlJobGroup.getAddressList().split(",");
            for (String item: addresss) {
                if (item==null || item.trim().length()==0) {
                    return new ReturnT<String>(500, I18nUtil.getString("jobgroup_field_registryList_unvalid") );
                }
            }
        }

        //保存更改日期
        xxlJobGroup.setUpdateTime(new Date());

        int ret = xxlJobGroupDao.save(xxlJobGroup);
        return (ret>0)?new ReturnT<>("增加执行器成功！"):ReturnT.FAIL;
    }

    @RequestMapping("/update")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<String> update(XxlJobGroup xxlJobGroup){

        //判断更改的信息是不是在有效范围内
        if (xxlJobGroup.getAppname()==null || xxlJobGroup.getAppname().trim().length()==0) {
            return new ReturnT<String>(500, (I18nUtil.getString("system_please_input")+"AppName") );
        }
        if (xxlJobGroup.getAppname().length()<4 || xxlJobGroup.getAppname().length()>64) {
            return new ReturnT<String>(500, I18nUtil.getString("jobgroup_field_appname_length") );
        }
        if (xxlJobGroup.getTitle()==null || xxlJobGroup.getTitle().trim().length()==0) {
            return new ReturnT<String>(500, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobgroup_field_title")) );
        }

        if(xxlJobGroup.getAddressType() == 0){   //0 =自动注册   自动注册会遍历表中匹配的地址并加逗号装回执行器表中
            List<String> registryList = findRegistryByAppName(xxlJobGroup.getAppname());
            String addressListStr = null;
            if(registryList != null && !registryList.isEmpty()){
                Collections.sort(registryList);  //排序
                addressListStr ="";
                for(String item : registryList){
                    addressListStr += item + ",";
                }
                addressListStr = addressListStr.substring(0,addressListStr.length()-1);
            }
            xxlJobGroup.setAddressList(addressListStr);
        }else{
            //1 = 手动注册
            if (xxlJobGroup.getAddressList()==null || xxlJobGroup.getAddressList().trim().length()==0) {
                return new ReturnT<String>(500, I18nUtil.getString("jobgroup_field_addressType_limit") );
            }
            String[] addresss = xxlJobGroup.getAddressList().split(",");

            //判断输入地址是否符合规范
            for(String item : addresss){
                if(item == null || item.trim().length() == 0){
                    return new ReturnT<String>(500, I18nUtil.getString("jobgroup_field_registryList_unvalid") );
                }
            }

        }

        //时间
        xxlJobGroup.setUpdateTime(new Date());

        int ret = xxlJobGroupDao.update(xxlJobGroup);
        return (ret > 0)?new ReturnT<>("更新执行器信息成功！"):ReturnT.FAIL;
    }


    /**
     * 搜索对应appname可以选顶的注册表地址
     * @param appnameParam
     * @return
     */
    private List<String> findRegistryByAppName(String appnameParam){

        HashMap<String, List<String>> appAddressMap = new HashMap<String , List<String>>();
        List<XxlJobRegistry> list = xxlJobRegistryDao.findAll(RegistryConfig.DEAD_TIMEOUT,new Date());
        if(list != null){
            for(XxlJobRegistry item : list){
                if(RegistryConfig.RegistType.EXECUTOR.name().equals(item.getRegistryGroup())){
                    String appname = item.getRegistryKey();
                    List<String> registryList = appAddressMap.get(appname);
                    if(registryList == null){
                        registryList = new ArrayList<String>();
                    }

                    //如果没有存的话就再创建新的存进去
                    if(!registryList.contains(item.getRegistryValue())){
                        registryList.add(item.getRegistryValue());
                    }
                    appAddressMap.put(appname, registryList);
                }
            }
        }
        return appAddressMap.get(appnameParam);
    }




    @RequestMapping("/remove")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<String> remove(int id){

        //判断是否可以删除
        int count = xxlJobInfoDao.pageListCount(0, 10, id, -1,  null, null, null);
        if (count > 0) {
            return new ReturnT<String>(500, I18nUtil.getString("jobgroup_del_limit_0") );
        }

        List<XxlJobGroup> alllist = xxlJobGroupDao.findAll();
        if(alllist.size() == 1){
            return new ReturnT<String>(500, I18nUtil.getString("jobgroup_del_limit_1") );
        }

        int ret = xxlJobGroupDao.remove(id);
        return  (ret > 0)?new ReturnT<>("删除执行器成功！"):ReturnT.FAIL;
    }

    @RequestMapping("/loadById")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<XxlJobGroup> loadById(int id){
        XxlJobGroup jobGroup = xxlJobGroupDao.load(id);
        return jobGroup!=null?new ReturnT<XxlJobGroup>(jobGroup):new ReturnT<XxlJobGroup>(ReturnT.FAIL_CODE, null);
    }
}
