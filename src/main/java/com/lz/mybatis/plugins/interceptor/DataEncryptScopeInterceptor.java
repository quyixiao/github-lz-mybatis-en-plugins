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

import com.lz.mybatis.plugins.interceptor.baomidou.SqlParserHandler;
import com.lz.mybatis.plugins.interceptor.baomidou.toolkit.PluginUtils;
import com.lz.mybatis.plugins.interceptor.utils.PSqlParseUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;

import java.sql.Statement;
import java.util.Properties;

@Slf4j
@Intercepts({@Signature(type = StatementHandler.class, method = "parameterize", args = {Statement.class})})
public class DataEncryptScopeInterceptor extends SqlParserHandler implements Interceptor {

    public DataEncryptScopeInterceptor() {

    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = (StatementHandler) PluginUtils.realTarget(invocation.getTarget());
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        // 先判断是不是SELECT操作
        BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        if (SqlCommandType.SELECT.equals(mappedStatement.getSqlCommandType())) {           //非select语句
            return invocation.proceed();
        } else {
            this.sqlParser(metaObject);
            ParameterHandler parameterHandler = (ParameterHandler) metaObject.getValue("delegate.parameterHandler");
            String mapperdId = PSqlParseUtil.getMapperId(mappedStatement);
            Configuration configuration = mappedStatement.getConfiguration();
            encrySqlParamData(configuration, parameterHandler, mapperdId, boundSql);
            Object result = invocation.proceed();
            return result;
        }
    }

    /**
     * 生成拦截对象的代理
     *
     * @param target 目标对象
     * @return 代理对象
     */
    @Override
    public Object plugin(Object target) {
        if (target instanceof StatementHandler) {
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
