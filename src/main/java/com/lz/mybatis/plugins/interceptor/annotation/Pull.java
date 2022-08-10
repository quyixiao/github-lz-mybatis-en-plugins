package com.lz.mybatis.plugins.interceptor.annotation;


import java.lang.annotation.*;


/**
 * 排序第4位
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Pull {

    // 自己对象的属性名
    String self() default "";

    // 要拉取对象的属性名 self = #{target}
    String target() default "";

    // order by id #{sort} ，默认情况下是以id 升序或降序排序，如果想要其他字段排序，
    // 可以写到where后面
    String sort() default "asc";

    // where  ${where }
    String where() default "";

    // limit #{limit}
    int limit() default 1000;

}
