package com.hy.job.admin.core.model;

import lombok.Data;

import java.util.Date;

@Data
public class XxlJobRegistry {

    private int id;
    private String registryGroup;
    private String registryKey;
    private String registryValue;
    private Date updateTime;
}
