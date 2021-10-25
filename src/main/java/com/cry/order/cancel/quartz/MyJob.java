package com.cry.order.cancel.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @author cry777
 * @program order-cancel
 * @description quartz实现: 通过一个线程定时的去扫描数据库，通过订单时间来判断是否有超时的订单，然后进行update或delete等操作
 * @create 2021-10-25
 */
public class MyJob implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        System.out.println("轮询数据库, 判断订单时间是否有超时");
        // 如果订单时间超时, 则执行update或delete操作
    }


}
