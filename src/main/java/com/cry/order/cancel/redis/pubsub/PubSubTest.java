package com.cry.order.cancel.redis.pubsub;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

/**
 * @author cry777
 * @program order-cancel
 * @description pub/sub实现, 需要在redis配置文件加上: notify-keyspace-events Ex
 * @create 2021-10-25
 */
public class PubSubTest {

    private static final String ADDR = "10.50.51.66";
    private static final int PORT = 6380;

    private static JedisPool jedis = new JedisPool(ADDR, PORT);
    private static RedisSub sub = new RedisSub();

    public static void init() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                jedis.getResource().subscribe(sub, "__keyevent@0__:expired");
            }
        }).start();
    }

    public static void main(String[] args)  {
        init();

        for(int i =0;i<10;i++){
            String orderId = "OID000000"+i;
            jedis.getResource().setex(orderId, 10, orderId);
            System.out.println(System.currentTimeMillis()+"ms:"+orderId+"订单生成");
        }
    }

    static class RedisSub extends JedisPubSub {
        @Override
        public void onMessage(String channel, String message) {
            System.out.println(System.currentTimeMillis()+"ms:"+message+"订单取消");
        }
    }
}
