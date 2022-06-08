package com.lidonghao.distributedlockdemo.util;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;

/**
 * http 请求工具类
 */
public class SimpleHttpClient {
    public static Logger logger = LoggerFactory.getLogger(SimpleHttpClient.class);

    /**
     * uri 保留字
     */
    private static BitSet URI_UNRESERVED_CHARACTERS = new BitSet();

    /**
     * 编码字符串
     */
    private static String[] PERCENT_ENCODED_STRINGS = new String[256];

    static {
        for (int i = 'a'; i <= 'z'; i++) {
            URI_UNRESERVED_CHARACTERS.set(i);
        }
        for (int i = 'A'; i <= 'Z'; i++) {
            URI_UNRESERVED_CHARACTERS.set(i);
        }
        for (int i = '0'; i <= '9'; i++) {
            URI_UNRESERVED_CHARACTERS.set(i);
        }
        URI_UNRESERVED_CHARACTERS.set('-');
        URI_UNRESERVED_CHARACTERS.set('.');
        URI_UNRESERVED_CHARACTERS.set('_');
        URI_UNRESERVED_CHARACTERS.set('~');
        for (int i = 0; i < PERCENT_ENCODED_STRINGS.length; ++i) {
            PERCENT_ENCODED_STRINGS[i] = String.format("%%%02X", i);
        }
    }

    /**
     * @方法名称 sendPut
     * @功能描述 <pre>put请求</pre>
     * @param url 请求地址(完整-含参数)
     * @param params 请求参数
     * @return 响应结果
     */
    public static String sendPut(String url, Map<String, Object> params) {
        DataOutputStream out = null;
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            HttpURLConnection connection = (HttpURLConnection)realUrl.openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("accept", "*/*");
            connection.setDoOutput(true);
            connection.connect();
            out = new DataOutputStream(connection.getOutputStream());
            out.write(getBodyStr(params).getBytes("UTF8"));
            out.flush();
            return getResponse(connection);
        } catch (IOException e) {
            e.printStackTrace();
            logger.warn(e.getMessage());
        } finally {
            if (null != out) {
                try {
                    out.close();
                } catch (IOException e) {
                    logger.warn(e.getMessage());
                }
            }
        }
        return "";
    }

    /**
     * @方法名称 sendDelete
     * @功能描述 <pre>delete请求</pre>
     * @param url 请求地址
     * @return 响应结果
     */
    public static String sendDelete(String url) {
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            HttpURLConnection connection = (HttpURLConnection)realUrl.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("DELETE");
            connection.connect();
            return getResponse(connection);
        } catch (IOException e) {
            logger.warn(e.getMessage());
        }
        return "";
    }

    /**
     * @方法名称 getResponse
     * @功能描述 <pre>获取响应结果</pre>
     * @param connection 请求链接
     * @return 响应结果
     */
    private static String getResponse(HttpURLConnection connection)
            throws IOException {
        // 定义 BufferedReader输入流来读取URL的响应
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuffer result = new StringBuffer();
        String line;
        while ((line = in.readLine()) != null) {
            result.append(line);
        }
        return result.toString();
    }

    /**
     * @方法名称 getBodyStr
     * @功能描述 <pre>获取请求体</pre>
     * @param param 请求参数
     * @return 请求体
     */
    public static String getBodyStr(Map<String, Object> param) {
        ArrayList<String> arr = new ArrayList<String>();
        for (Map.Entry<String, Object> entry : param.entrySet()) {
            if (entry.getValue() == null || entry.getValue().equals("")) {
                arr.add(uriEncode(entry.getKey(), true));
            } else {
                arr.add(String.format("%s=%s", uriEncode(entry.getKey(), true), uriEncode(entry.getValue().toString(), true)));
            }
        }
        Iterator<String> iter = arr.iterator();
        if (!iter.hasNext()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        while (iter.hasNext()) {
            String item = iter.next();
            builder.append(item);
            builder.append('&');
        }
        builder.deleteCharAt(builder.length() - 1); // remove last sep
        return builder.toString();
    }

    /**
     * @方法名称 uriEncode
     * @功能描述 <pre>uri编码</pre>
     * @param value 原始值
     * @param encodeSlash "/"是否编码
     * @return 结果值
     */
    public static String uriEncode(String value, boolean encodeSlash) {
        try {
            StringBuilder builder = new StringBuilder();
            for (byte b : value.getBytes("UTF8")) {
                if (URI_UNRESERVED_CHARACTERS.get(b & 0xFF)) {
                    builder.append((char)b);
                } else {
                    builder.append(PERCENT_ENCODED_STRINGS[b & 0xFF]);
                }
            }
            String encodeString = builder.toString();
            if (!encodeSlash) {
                return encodeString.replace("%2F", "/");
            }
            return encodeString;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
