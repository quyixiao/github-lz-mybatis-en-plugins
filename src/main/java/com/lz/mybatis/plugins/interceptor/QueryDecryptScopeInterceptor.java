/*
 *    Copyright (c) 2018-2025, songfayuan All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of the 霖梓控股 developer nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * Author: songfayuan (1414798079@qq.com)
 */

package com.lz.mybatis.plugins.interceptor;

import ch.qos.logback.classic.Logger;
import com.lz.mybatis.plugins.interceptor.baomidou.toolkit.PluginUtils;
import com.lz.mybatis.plugins.interceptor.utils.*;
import com.lz.mybatis.plugins.interceptor.utils.t.PPTuple;
import com.lz.mybatis.plugins.interceptor.utils.t.PTuple2;
import com.mysql.jdbc.JDBC42PreparedStatement;
import com.mysql.jdbc.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.Proxy;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author songfayuan
 * @date 2018/1/19
 * 数据权限插件，guns
 */
@Slf4j
@Intercepts({@Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class})})
public class QueryDecryptScopeInterceptor implements Interceptor {

    public static Map<String, PPTuple> outMap = new ConcurrentHashMap<>(256);

    /**
     * 代替拦截对象的方法内容
     * 责任链对象
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        DefaultResultSetHandler defaultResultSetHandler = (DefaultResultSetHandler) PluginUtils.realTarget(invocation.getTarget());
        //MappedStatement维护了一条<select|update|delete|insert>节点的封装
        MetaObject metaObject = SystemMetaObject.forObject(defaultResultSetHandler);
        // 先判断是不是SELECT操作
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("mappedStatement");

        //获取当前resutType的类型
        Object object = realTarget(invocation.getArgs()[0]);
        com.mysql.jdbc.Field[] fields = null;
        //BoundSql对象是处理SQL语句用的
        MetaObject metaObjectResult = SystemMetaObject.forObject(object);
        if (object != null) {
            if (object instanceof JDBC42PreparedStatement) {
                fields = (com.mysql.jdbc.Field[]) metaObjectResult.getValue("results.fields");
            } else if ("DruidPooledPreparedStatement".equals(object.getClass().getSimpleName())) {
                Object obx = metaObjectResult.getValue("stmt");
                if (obx != null && "PreparedStatementProxyImpl".equals(obx.getClass().getSimpleName())) {
                    fields = (com.mysql.jdbc.Field[]) metaObjectResult.getValue("stmt.statement.results.fields");
                } else {
                    fields = (com.mysql.jdbc.Field[]) metaObjectResult.getValue("stmt.results.fields");
                }
            } else {
                log.info("查询没有捕捉到的类：" + object.getClass().getSimpleName());
            }
        }
        Object result = invocation.proceed();
        String mapperdId = PSqlParseUtil.getMapperId(mappedStatement);
        BoundSql boundSql = (BoundSql) metaObject.getValue("boundSql");
        try {
            if (result != null) {
                PTuple2<Boolean, List<String>> data = getChangeColumn(fields, mapperdId, boundSql.getSql()).getData();
                if (data.getFirst()) {
                    if (result instanceof Collection) {
                        forceChageList((Collection) result, data.getSecond());
                    } else if (result.getClass().isArray()) {     //如果是数组类型
                        forceChageList(Arrays.asList((Object[]) result), data.getSecond());
                    } else {
                        forceChange(result, data.getSecond());
                    }
                }
            }
        } catch (Exception e) {
            log.error("sql select 解密数据异常 exception ", e);
            PDingDingUtils.sendText("异常编号 =" + Logger.inheritableThreadLocalNo.get()
                    + "\n mapperId =" + mapperdId
                    + "\n sql = " + boundSql.getSql()
                    + "\n 异常堆栈 = " + PExceptionUtils.dealException(e));
        }
        return result;
    }

    public PPTuple getChangeColumn(com.mysql.jdbc.Field[] fields, String mapperdId, String sql) throws Exception {
        String key = mapperdId + PMD5Util.encode(sql);
        PPTuple pluginTuple = outMap.get(key);
        if (pluginTuple != null) {
            return pluginTuple;
        }
        if (fields == null) {
            pluginTuple = new PPTuple(false, null);
            outMap.put(key, pluginTuple);
            return pluginTuple;
        }
        List<String> aliaNames = new ArrayList<>();
        Map<String, PPTuple> tableConfig = EncryptTableConfig.tableConfig;
        for (com.mysql.jdbc.Field field : fields) {
            String tableName = field.getOriginalTableName();
            if (tableConfig.containsKey(tableName)) {                               //正常处理情况
                PTuple2<List<String>, List<String>> data = tableConfig.get(tableName).getData();
                List<String> columnNames = data.getFirst();
                if (columnNames.contains(field.getOriginalName())) {
                    aliaNames.add(PSqlParseUtil.field2JavaCode(field.getName()));
                }
            } else if (StringUtils.isNullOrEmpty(tableName)) {        //对于原始表名丢失的情况
                aliaNames.add(PSqlParseUtil.field2JavaCode(field.getName()));
            }
        }
        pluginTuple = new PPTuple(aliaNames.size() > 0, aliaNames);
        if (!("?".equals(sql.trim()))) {
            outMap.put(key, pluginTuple);
        }
        return pluginTuple;
    }

    public void forceChageList(Collection collection, List<String> fields) {
        if (collection instanceof List) {
            List list = (List) collection;
            if (list != null && list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {
                    Object o = list.get(i);
                    if (o instanceof String) {
                        list.set(i, decode(o.toString()));
                    } else {
                        forceChange(o, fields);
                    }
                }
            }
        } else {
            for (Object o : collection) {
                forceChange(o, fields);
            }
        }
    }

    public void forceChange(Object o, List<String> fields) {
        MetaObject metaO = SystemMetaObject.forObject(o);
        for (String field : fields) {
            Object x = null;
            try {
                x = metaO.getValue(field);
            } catch (Exception e) {
            }
            if (x != null) {
                metaO.setValue(field, decode(x));
            } else {
                String underLinecase = PSqlParseUtil.toUnderlineCase(field);
                try {
                    x = metaO.getValue(underLinecase);
                } catch (Exception e) {

                }
                if (x != null) {
                    metaO.setValue(underLinecase, decode(x));
                }
            }
        }
    }

    public static Object decode(Object indetValue) {
        if (indetValue instanceof String) {
            try {
                if (indetValue == null || PStringUtils.isEmpty(indetValue.toString())) {
                    return indetValue;
                }

                String object = PAesUtil.decrypt((String) indetValue);
                if (PStringUtils.isBlank(object) && PStringUtils.isNotBlank(((String) indetValue))) {
                    return indetValue;
                }
                return object;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return indetValue;
    }


    public static Object realTarget(Object target) {
        if (Proxy.isProxyClass(target.getClass())) {
            MetaObject metaObject = SystemMetaObject.forObject(target);
            return realTarget(metaObject.getValue("h.statement.stmt"));
        }
        return target;
    }


    /**
     * 生成拦截对象的代理
     *
     * @param target 目标对象
     * @return 代理对象
     */
    @Override
    public Object plugin(Object target) {
        if (target instanceof ResultSetHandler) {
            //使用MyBatis提供的Plugin类生成代理对象
            return Plugin.wrap(target, this);
        }
        return target;
    }

    /**
     * @param properties mybatis获取插件的属性，我们在MyBatis配置文件里配置的
     */
    @Override
    public void setProperties(Properties properties) {

    }


}
