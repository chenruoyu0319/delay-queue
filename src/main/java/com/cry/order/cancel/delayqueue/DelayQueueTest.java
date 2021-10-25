package com.cry.order.cancel.delayqueue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author cry777
 * @program order-cancel
 * @description
 * @create 2021-10-25
 */
public class DelayQueueTest {
    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("00000001");
        list.add("00000002");
        list.add("00000003");
        list.add("00000004");
        list.add("00000005");

        DelayQueue<OrderDelay> queue = new DelayQueue<>();

        long start = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {

            //延迟三秒取出
            queue.put(new OrderDelay(list.get(i),
                    TimeUnit.NANOSECONDS.convert(3, TimeUnit.SECONDS)));
            try {
                queue.take().print();
                System.out.println("After " +
                        (System.currentTimeMillis() - start) + " MilliSeconds");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
