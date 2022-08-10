package com.lz.mybatis.plugins.interceptor.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class PExceptionUtils {


    public static String dealException(Exception e) {
        return getExceptionInfo(e);
    }


    public static String dealException(Throwable e) {
        return getExceptionInfo(e);
    }

    private static String getExceptionInfo(Throwable e) {
        StringWriter sw = null;
        try {
            e.printStackTrace();
            sw = new StringWriter();
            //将出错的栈信息输出到printWriter中
            e.printStackTrace(new PrintWriter(sw, true));
            String str = sw.toString();
            sw.flush();
            return str;
        } finally {
            if (sw != null) {
                try {
                    sw.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }


}
