package com.lidonghao.distributedlockdemo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * CURL命令辅助类
 */
public class CurlUtil {
    public static Logger logger = LoggerFactory.getLogger(CurlUtil.class);

    /**
     * 默认命令头
     */
    private static final String CURL = "curl";

    /**
     * @方法名称 execCurl
     * @功能描述 <pre>执行命令</pre>
     * @param cmds java curl不支持空格，所有用空格分割命令
     * @return 执行结果：null-错误/检查操作，""-无返回值
     */
    public static String execCurl(List<String> cmds)
            throws RuntimeException {
        if (null == cmds || cmds.size() == 0) {
            return null;
        }
        if (!cmds.get(0).equals(CURL)) {
            cmds.add(0, CURL);
        }
        ProcessBuilder process = new ProcessBuilder(cmds);
        try {
            Process p = process.start();
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = null;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append(System.getProperty("line.separator"));
                }
                logger.debug(Thread.currentThread().getName().concat(":").concat(builder.toString().trim()));
            }
            return builder.toString().trim();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
