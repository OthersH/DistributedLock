package com.lidonghao.distributedlockdemo;

import com.lidonghao.distributedlockdemo.exception.LockException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DistributedLockDemoApplicationTests {

    @Test
    void redisLock() throws Exception {
        LockClient nxClient = Lock.factory().redisSolution().build();

        Lock lock = nxClient.newLock("firstLock16");
        System.err.println("22222222aaa：" + lock.acquire(10));
//        Thread.sleep(1100);
        lock.release();
//
//        nxClient.close();
        Thread thread = new Thread() {
            @Override
            public void run() {
                Lock lock2 = null;
                try {
                    lock2 = nxClient.newLock("firstLock16");
                    System.err.println("33333aaa" + lock2.acquire(10));
                } catch (LockException e) {
                    throw new RuntimeException(e);
                }

            }
        };
        thread.start();
    }

    @Test
    void etcdLock() throws Exception {
        LockClient nxClient = Lock.factory().etcdSolution().connectionString("http://127.0.0.1:2379").clusterName("monitor-server").build();

        Lock lock = nxClient.newLock("firstLock16");
        System.err.println("22222222aaa：" + lock.acquire(10));
//        Thread.sleep(1100);
        lock.release();
//
//        nxClient.close();
        Thread thread = new Thread() {
            @Override
            public void run() {
                Lock lock2 = null;
                try {
                    lock2 = nxClient.newLock("firstLock16");
                    System.err.println("33333aaa" + lock2.acquire(10));
                } catch (LockException e) {
                    throw new RuntimeException(e);
                }

            }
        };
        thread.start();
    }

    @Test
    void zookeeperLock() throws Exception {
        LockClient nxClient = Lock.factory().zookeeperSolution().connectionUrl("127.0.0.1:2181").build();

        Lock lock = nxClient.newLock("firstLock16");
        System.err.println("22222222aaa：" + lock.acquire(10));
//        Thread.sleep(1100);
        lock.release();
//
//        nxClient.close();
        Thread thread = new Thread() {
            @Override
            public void run() {
                Lock lock2 = null;
                try {
                    lock2 = nxClient.newLock("firstLock16");
                    System.err.println("33333aaa" + lock2.acquire(10));
                } catch (LockException e) {
                    throw new RuntimeException(e);
                }

            }
        };
        thread.start();
    }
}
