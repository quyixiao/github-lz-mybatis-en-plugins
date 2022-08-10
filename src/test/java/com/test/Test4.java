package com.test;

import com.lz.mybatis.plugins.interceptor.MapF2FInterceptor;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class Test4 {


    public Map<String, List<Test4>> getList() {


        return null;
    }

    public static void main(String[] args) throws Exception {
         Method method = Test4.class.getDeclaredMethod("getList");
        System.out.println(method);


        Object a = MapF2FInterceptor.getActualType(method,1,0);
        System.out.println(a);
    }
}
