package com.lidonghao.distributedlockdemo.client;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * redis 客户端：居于jedis实现
 */
public class RedisClient {
    /**
     * 数据库索引-key
     */
    private final static String REDIS_DB_KEY = "redis.db";

    /**
     * 集群模式-key
     */
    private final static String REDIS_MODE_KEY = "redis.mode";

    /**
     * 服务器
     */
    protected final static String CACHE_HOST_KEY = "cache.host";

    /**
     * 服务器端口
     */
    protected final static String CACHE_PORT_KEY = "cache.port";

    /**
     * 用户
     */
    protected final static String CACHE_USER_KEY = "cache.user";

    /**
     * 密码
     */
    protected final static String CACHE_PWD_KEY = "cache.pwd";

    /**
     * 是否集群
     */
    protected final static String CLUSTER_KEY = "cache.cluster";

    /**
     * 集群服务器列表，以“,”分隔
     */
    protected final static String CLUSTER_IP_KEY = "cache.cluster.ip";

    /**
     * 最大空闲连接数
     */
    protected final static String CACHE_MAXIDLE_KEY = "cache.maxidle";

    /**
     * 最大连接数
     */
    protected final static String CACHE_MAXTOTAL_KEY = "cache.maxtotal";

    /**
     * 最大等待毫秒数(获取连接时,阻塞时启用)
     */
    protected final static String CACHE_MAXWAITMILLIS_KEY = "cache.maxwaitmillis";

    /**
     * 最小空闲连接数
     */
    protected final static String CACHE_MINIDLE_KEY = "cache.minidle";

    /**
     * 连接耗尽时是否阻塞(false报异常,ture阻塞直到超时, 默认true)
     */
    protected final static String CACHE_BLOCK_KEY = "cache.block";

    /**
     * 是否启用pool的jmx管理功能
     */
    private final static String REDIS_JMX_ENABLED_KEY = "redis.jmx.enabled";

    private final static String REDIS_JMX_PREFIX_KEY = "redis.jmx.prefix";

    /**
     * 是否启用后进先出, 默认true
     */
    private final static String REDIS_LIFO_KEY = "redis.lifo";

    /**
     * 逐出策略类名
     */
    private final static String REDIS_EVICTION_CLASS_KEY = "redis.eviction.class";

    /**
     * 逐出连接的最小空闲时间
     */
    private final static String REDIS_EVICTION_MINIDLETIMEMILLIS_KEY = "redis.eviction.minidletimemillis";

    /**
     * 每次逐出的最大数目
     */
    private final static String REDIS_EVICTION_NUM_KEY = "redis.eviction.num";

    /**
     * 对象空闲多久后逐出
     */
    private final static String REDIS_EVICTION_OBJECT_MINIDLETIMEMILLIS_KEY = "redis.eviction.object.minidletimemillis";

    /**
     * 逐出扫描的时间间隔(毫秒)
     */
    private final static String REDIS_EVICTION_RUNSTIME_KEY = "redis.eviction.runstime";

    /**
     * 在获取连接的时候检查有效性
     */
    private final static String REDIS_TEST_BORROW_KEY = "redis.test.borrow";

    /**
     * 在空闲时检查有效性
     */
    private final static String REDIS_TEST_IDLE_KEY = "redis.test.idle";

    /**
     * 集群标识
     */
    private final static String CLUSTER_TRUE = "true";

    /**
     * 集群模式-分布式集群
     */
    private final static String MODE_CLUSTER = "cluster";

    /**
     * 集群模式-分片集群
     */
    private final static String MODE_SHARDED = "sharded";

    /**
     * redis默认配置
     */
    private static Properties redisDefault;

    /**
     * redis自定义配置
     */
    private static Properties redisCustom;

    /**
     * 链接池配置信息
     */
    private static GenericObjectPoolConfig config = new GenericObjectPoolConfig();

    /**
     * 单机链接池
     */
    private static JedisPool jedisPool = null;

    /**
     * 分片链接池
     */
    private static ShardedJedisPool shardedJedisPool = null;

    /**
     * 集群链接池
     */
    private JedisCluster jedisCluster = null;

    static {
        InputStream defaultStream = RedisClient.class.getResourceAsStream("/config/redis-default.properties");// 加载 默认配置
        if (null != defaultStream) {
            try {
                redisDefault = new Properties();
                redisDefault.load(defaultStream);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    defaultStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        InputStream cacheStream = RedisClient.class.getResourceAsStream("/redis.properties");// 加载 自定义配置
        if (null != cacheStream) {
            try {
                redisCustom = new Properties();
                redisCustom.load(cacheStream);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    cacheStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        config.setMaxIdle(Integer.valueOf(getRedisProp(CACHE_MAXIDLE_KEY)));// 最大空闲连接数
        config.setMaxTotal(Integer.valueOf(getRedisProp(CACHE_MAXTOTAL_KEY))); // 最大连接数
        config.setMinIdle(Integer.valueOf(getRedisProp(CACHE_MINIDLE_KEY)));// 最小空闲连接数
        config.setMaxWaitMillis(Integer.valueOf(getRedisProp(CACHE_MAXWAITMILLIS_KEY)));// 最大等待毫秒数
        config.setBlockWhenExhausted(Boolean.valueOf(getRedisProp(CACHE_BLOCK_KEY)));// 连接耗尽时是否阻塞

        config.setJmxEnabled(Boolean.valueOf(getRedisProp(REDIS_JMX_ENABLED_KEY)));// 是否启用pool的jmx管理功能
        config.setJmxNamePrefix(getRedisProp(REDIS_JMX_PREFIX_KEY));

        config.setLifo(Boolean.valueOf(getRedisProp(REDIS_LIFO_KEY)));// 是否启用后进先出
        config.setEvictionPolicyClassName(getRedisProp(REDIS_EVICTION_CLASS_KEY));// 逐出策略类名
        config.setMinEvictableIdleTimeMillis(Integer.valueOf(getRedisProp(REDIS_EVICTION_MINIDLETIMEMILLIS_KEY)));// 逐出连接的最小空闲时间
        config.setNumTestsPerEvictionRun(Integer.valueOf(getRedisProp(REDIS_EVICTION_NUM_KEY)));// 每次逐出的最大数目
        config.setSoftMinEvictableIdleTimeMillis(Integer.valueOf(getRedisProp(REDIS_EVICTION_OBJECT_MINIDLETIMEMILLIS_KEY)));// 对象空闲多久后逐出
        config.setTimeBetweenEvictionRunsMillis(Integer.valueOf(getRedisProp(REDIS_EVICTION_RUNSTIME_KEY))); // 逐出扫描的时间间隔

        config.setTestOnBorrow(Boolean.valueOf(getRedisProp(REDIS_TEST_BORROW_KEY)));// 在获取连接的时候检查有效性
        config.setTestWhileIdle(Boolean.valueOf(getRedisProp(REDIS_TEST_IDLE_KEY)));// 在空闲时检查有效性
    }

    protected void init() {
        String host = getRedisProp(CACHE_HOST_KEY);
        int port = Integer.valueOf(getRedisProp(CACHE_PORT_KEY));
        String pwd = getRedisProp(CACHE_PWD_KEY);
        pwd = null == pwd || "".equals(pwd.trim()) ? null : pwd;
        int db = Integer.valueOf(getRedisProp(REDIS_DB_KEY));
        if (CLUSTER_TRUE.equals(getRedisProp(CLUSTER_KEY))) {
            String[] ips = getRedisProp(CLUSTER_IP_KEY).split(",");
            if (MODE_SHARDED.equals(getRedisProp(REDIS_MODE_KEY))) {// 分片 集群
                List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
                JedisShardInfo master = new JedisShardInfo(host, port, "master");
                if (null != pwd) {
                    master.setPassword(pwd);
                }
                shards.add(master);
                for (String ip : ips) {
                    if (!ip.trim().equals("")) {
                        JedisShardInfo shardInfo = new JedisShardInfo(ip, port, "slave_" + ip);
                        if (null != pwd) {
                            shardInfo.setPassword(pwd);
                        }
                        shards.add(shardInfo);
                    }
                }
                shardedJedisPool = new ShardedJedisPool(config, shards);
            } else if (MODE_CLUSTER.equals(getRedisProp(REDIS_MODE_KEY))) {// 分布式 集群
                Set<HostAndPort> jedisClusterNodes = new HashSet<HostAndPort>();
                for (String ip : ips) {
                    if (!ip.trim().equals("")) {
                        jedisClusterNodes.add(new HostAndPort(ip, port));
                    }
                }
                jedisCluster = new JedisCluster(jedisClusterNodes, 2000, 2000, 5, pwd, config);
            }
        } else {
            jedisPool = new JedisPool(config, host, port, 30000, pwd, db);
        }
    }

    private volatile static RedisClient client;

    private RedisClient() {
        init();
    }

    /**
     * 双锁单例
     */
    public static RedisClient createInstance() {
        if (client == null) {
            synchronized (RedisClient.class) {
                if (client == null) {
                    client = new RedisClient();
                }
            }
        }
        return client;
    }

    /**
     * @方法名称 getRedisProp
     * @功能描述 <pre>获取redis的配置</pre>
     * @param key 配置key
     * @return 配置值
     */
    public static String getRedisProp(String key) {
        return null != redisCustom && null != redisCustom.getProperty(key) ? redisCustom.getProperty(key) : redisDefault.getProperty(key);
    }

    /**
     *
     * @类名称 RedisClient.java
     * @类描述 <pre>Jedis 命令对象</pre>
     */
    protected abstract class Command {
        /**
         * @方法名称 exeLife
         * @功能描述 <pre>运行生命周期</pre>
         * @return 执行结果
         */
        @SuppressWarnings("unchecked")
        public <T> T exeLife() {
            T obj = null;
            if (null != jedisPool) {
                Jedis jedis = jedisPool.getResource();
                try {
                    obj = (T)exe(jedis);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    jedis.close();
                }
            } else if (null != shardedJedisPool) {
                ShardedJedis shardedJedis = shardedJedisPool.getResource();
                try {
                    obj = (T)exe(shardedJedis);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    shardedJedis.close();
                }
            } else if (null != jedisCluster) {
                obj = (T)exe(jedisCluster);
            }
            return obj;
        }

        /**
         * @方法名称 exe
         * @功能描述 <pre>执行命令</pre>
         * @param commands 命令对象
         * @return 返回执行结果
         */
        public abstract Object exe(JedisCommands commands);
    }

    /**
     * @方法名称 setNxPx
     * @功能描述 set NX PX
     * @param key 键值
     * @param value 缓存值
     * @param second 过期时间，单位秒
     * @return 成功-"OK"，失败-"null"
     */
    public String setNxPx(String key, String value, int second) {
        return new Command() {
            @Override
            public String exe(JedisCommands commands) {
                String realKey = getRealKey(key);
                return commands.set(realKey, value, "NX", "EX", second);
            }
        }.exeLife();
    }

    /**
     * @方法名称 expire
     * @功能描述 设置过期时间
     * @param key 键值
     * @param second 过期时间，单位秒
     * @return 成功-1 失败-0
     */
    public Long expire(String key, int second) {
        return new Command() {
            @Override
            public Long exe(JedisCommands commands) {
                return commands.expire(key, second);
            }
        }.exeLife();
    }

    /**
     * @方法名称 delete
     * @功能描述 删除缓存项(校验value)
     * @param key  键值
     * @param value 缓存值
     * @return 成功-1 失败-0
     */
    public Long delete(String key, String value) {
        /*
         * TODO: 此处需要用 lua才能保证原子性，否则有一定几率误删锁，增加AP Redis 多重获锁的几率。
         */
        return new Command() {
            @Override
            public Long exe(JedisCommands commands) {
                if (commands.exists(key) && commands.get(key).equals(value)) {
                    return commands.del(key);
                }
                return 0L;
            }
        }.exeLife();
    }

    public static String getRealKey(String key) {
        return key.length() > 32 ? DigestUtils.md5DigestAsHex(key.getBytes()) : key;
    }
}
