package com.lz.mybatis.plugins.interceptor.annotation;


import java.lang.annotation.*;


/**
 * 排序第4位
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Bean2Map {

    String key()[] default "id";

    String value() default "this";

    // 填充空对象 ，如果对象不存在 ,则new 一个对象，所有属性值为空
    boolean fillNull() default true;
}
