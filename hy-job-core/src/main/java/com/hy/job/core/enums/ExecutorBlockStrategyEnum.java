package com.hy.job.core.enums;

/**
 * @Author: HY
 * @Date: 2023-10-07-22:01
 * @Description:阻塞处理策略
 */
public enum ExecutorBlockStrategyEnum {

    SERIAL_EXECUTION("Serial execution"),   //串行执行策略   每次只执行一个任务，等待任务执行完成后再执行下一个任务
    /*CONCURRENT_EXECUTION("并行"),*/
    DISCARD_LATER("Discard Later"),         //后台丢弃策略  如果执行器忙于处理当前任务，会放弃后续的任务，直到当前任务执行完成后再处理下一个任务
    COVER_EARLY("Cover Early");            //覆盖策略   采用覆盖策略，即如果执行器忙于处理当前任务，会终止当前任务并立即执行新的任务

    private String title;
    private ExecutorBlockStrategyEnum (String title) {
        this.title = title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public String getTitle() {
        return title;
    }

    public static ExecutorBlockStrategyEnum match(String name, ExecutorBlockStrategyEnum defaultItem) {
        if (name != null) {
            for (ExecutorBlockStrategyEnum item:ExecutorBlockStrategyEnum.values()) {
                if (item.name().equals(name)) {
                    return item;
                }
            }
        }
        return defaultItem;
    }
}
