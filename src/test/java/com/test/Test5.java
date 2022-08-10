package com.test;

import com.lz.mybatis.plugins.interceptor.MapF2FInterceptor;

import java.lang.reflect.Field;
import java.util.List;

public class Test5 {

    private  List<Test> list ;

    public static void main(String[] args) {
        Field [] fields = Test5.class.getDeclaredFields();
        for(Field field : fields){
            Class x = MapF2FInterceptor.getFieldActualType(field,0);
            System.out.println(x);
        }
    }
}
