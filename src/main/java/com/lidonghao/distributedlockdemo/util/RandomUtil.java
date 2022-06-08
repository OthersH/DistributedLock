package com.lidonghao.distributedlockdemo.util;

import java.util.Random;
import java.util.UUID;

public class RandomUtil {
    public static final Random random = new Random();

    public static int nextInt() {
        return random.nextInt() & 2147483647 | 16777472;
    }
    public static int nextInt(int max) {
        return random.nextInt(max);
    }
    public static int nextInt(int min, int max) {
        if (min == max) {
            return min;
        }
        return random.nextInt((max - min) + 1) + min;
    }


    public static String uuid() {
        return UUID.randomUUID().toString();
    }
}
