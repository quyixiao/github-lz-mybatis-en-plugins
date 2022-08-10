package com.test;


import com.lz.mybatis.plugins.interceptor.utils.PStringUtils;

public class Test {



    public static String  substringBeforeLast(String mappenrdId ){
        int i  = mappenrdId.lastIndexOf(".");
        return mappenrdId.substring(0,i);
    }


    public static String  substringAfterLast(String mappenrdId ){
        int i  = mappenrdId.lastIndexOf(".");
        return mappenrdId.substring(i+1);
    }


    public static void main(String[] args) {
        System.out.println(substringBeforeLast("abcd.aa.cc.XX"));
        System.out.println(substringAfterLast("abcd.aa.cc.XX"));
    }


}
