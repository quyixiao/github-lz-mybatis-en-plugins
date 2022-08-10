/**
 * Copyright (c) 2020 fumeiai All rights reserved.
 * <p>
 * <p>
 * <p>
 * 版权所有，侵权必究！
 */

package com.lz.mybatis.plugins.interceptor.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring Context 工具类
 *
 * @author Mark sunlightcs@gmail.com
 */
@Component
public class PSpringContextUtils implements ApplicationContextAware {
    public static ApplicationContext applicationContext;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        PSpringContextUtils.applicationContext = applicationContext;
    }


    public static Object getBean(String name) {
        return applicationContext.getBean(name);
    }

    /// 获取当前环境
    public static String getActiveProfile() {
        if (applicationContext == null) {
            return null;
        }
        return applicationContext.getEnvironment().getActiveProfiles()[0];
    }

    public static <T> T getBean(String name, Class<T> requiredType) {
        return applicationContext.getBean(name, requiredType);
    }

    public static boolean containsBean(String name) {
        return applicationContext.containsBean(name);
    }


    public static boolean isSingleton(String name) {
        return applicationContext.isSingleton(name);
    }


    public static Class<? extends Object> getType(String name) {
        return applicationContext.getType(name);
    }


}
