package com.cry.order.cancel.delayqueue;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * @author cry777
 * @program order-cancel
 * @description JDK延迟队列实现
 * @create 2021-10-25
 */
public class OrderDelay implements Delayed {

    /**
     * 订单id
     */
    private String orderId;

    /**
     * 延迟时间
     */
    private long timeout;

    public OrderDelay(String orderId, long timeout) {
        this.orderId = orderId;
        this.timeout = timeout + System.nanoTime();
    }

    @Override
    public int compareTo(Delayed other) {
        if (this.equals(other)) {
            return 0;
        }

        OrderDelay t = (OrderDelay) other;
        long d = (this.getDelay(TimeUnit.NANOSECONDS) - t
                .getDelay(TimeUnit.NANOSECONDS));

        return (d == 0) ? 0 : ((d < 0) ? -1 : 1);
    }

    // 返回距离你自定义的超时时间还有多少
    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(timeout - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    void print() {
        System.out.println(orderId + "编号的订单要删除啦。。。。");
    }

}
