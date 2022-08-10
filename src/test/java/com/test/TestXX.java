package com.test;

import com.lz.mybatis.plugins.interceptor.QueryDecryptScopeInterceptor;

public class TestXX {
    public static void main(String[] args) {
        System.out.println(QueryDecryptScopeInterceptor.decode("893283298"));
        System.out.println(QueryDecryptScopeInterceptor.decode("abafa"));
        System.out.println(QueryDecryptScopeInterceptor.decode("dada"));
        System.out.println(QueryDecryptScopeInterceptor.decode("fa"));
        System.out.println(QueryDecryptScopeInterceptor.decode("a"));
        System.out.println(QueryDecryptScopeInterceptor.decode("c"));
        System.out.println(QueryDecryptScopeInterceptor.decode("a"));
        System.out.println(QueryDecryptScopeInterceptor.decode(""));
        System.out.println("-------------------");
    }
}
