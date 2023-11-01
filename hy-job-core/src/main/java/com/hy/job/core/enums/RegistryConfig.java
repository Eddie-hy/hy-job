package com.hy.job.core.enums;

/**
 * @Author: HY
 * @Date: 2023-10-06-22:16
 * @Description:注册表类
 */
public class RegistryConfig {

    public static final int BEAT_TIMEOUT = 30;
    public static final int DEAD_TIMEOUT = BEAT_TIMEOUT * 3;

    public enum RegistType{ EXECUTOR, ADMIN }
}
