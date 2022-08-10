/**
 * Copyright (c) 2011-2020, hubin (jobob@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.lz.mybatis.plugins.interceptor.baomidou;


import ch.qos.logback.classic.Logger;
import com.alibaba.fastjson.JSON;
import com.lz.druid.stat.TableStat;
import com.lz.mybatis.plugins.interceptor.EncryptTableConfig;
import com.lz.mybatis.plugins.interceptor.baomidou.entity.SqlInfo;
import com.lz.mybatis.plugins.interceptor.baomidou.entity.SqlParserInfo;
import com.lz.mybatis.plugins.interceptor.baomidou.plugins.ISqlParser;
import com.lz.mybatis.plugins.interceptor.baomidou.plugins.ISqlParserFilter;
import com.lz.mybatis.plugins.interceptor.baomidou.toolkit.PluginUtils;
import com.lz.mybatis.plugins.interceptor.entity.TableInfo;
import com.lz.mybatis.plugins.interceptor.mapper.TableInfoRowMapper;
import com.lz.mybatis.plugins.interceptor.utils.*;
import com.lz.mybatis.plugins.interceptor.utils.t.PPTuple;
import com.lz.mybatis.plugins.interceptor.utils.t.PTuple2;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.defaults.DefaultSqlSession;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * <p>
 * SQL 解析处理器
 * </p>
 *
 * @author hubin
 * @Date 2016-08-31
 */
@Slf4j
public abstract class SqlParserHandler {

    private List<ISqlParser> sqlParserList;
    private ISqlParserFilter sqlParserFilter;

    public static Map<String, PPTuple> enterMap = new ConcurrentHashMap<>(2048);

    /**
     * 拦截 SQL 解析执行
     */
    protected void sqlParser(MetaObject metaObject) {
        if (null != metaObject) {
            if (null != this.sqlParserFilter && this.sqlParserFilter.doFilter(metaObject)) {
                return;
            }
            // SQL 解析
            if (CollectionUtils.isNotEmpty(this.sqlParserList)) {
                // @SqlParser(filter = true) 跳过该方法解析
                SqlParserInfo sqlParserInfo = PluginUtils.getSqlParserInfo(metaObject);
                if (null != sqlParserInfo && sqlParserInfo.getFilter()) {
                    return;
                }
                // 标记是否修改过 SQL
                int flag = 0;
                String originalSql = (String) metaObject.getValue(PluginUtils.DELEGATE_BOUNDSQL_SQL);
                for (ISqlParser sqlParser : this.sqlParserList) {
                    SqlInfo sqlInfo = sqlParser.optimizeSql(metaObject, originalSql);
                    if (null != sqlInfo) {
                        originalSql = sqlInfo.getSql();
                        ++flag;
                    }
                }
                if (flag >= 1) {
                    metaObject.setValue(PluginUtils.DELEGATE_BOUNDSQL_SQL, originalSql);
                }
            }
        }
    }

    public List<ISqlParser> getSqlParserList() {
        return sqlParserList;
    }

    public SqlParserHandler setSqlParserList(List<ISqlParser> sqlParserList) {
        this.sqlParserList = sqlParserList;
        return this;
    }

    public ISqlParserFilter getSqlParserFilter() {
        return sqlParserFilter;
    }

    public void setSqlParserFilter(ISqlParserFilter sqlParserFilter) {
        this.sqlParserFilter = sqlParserFilter;
    }


    public void encrySqlParamData(Configuration configuration, ParameterHandler parameterHandler, String mapperdId, BoundSql boundSql) {
        PTuple2<Boolean, Map<Integer, String>> data = null;
        String sql = boundSql.getSql();
        String key = mapperdId + PMD5Util.encode(sql);
        try {
            PPTuple pluginTuple = enterMap.get(key);
            if (pluginTuple == null) {
                data = getChangeColumn(configuration, mapperdId, sql, key).getData();
            } else {
                data = pluginTuple.getData();
            }
            if (!data.getFirst()) {
                return;
            }
            Map<Integer, String> paramMaps = data.getSecond();
            Object parameterObject = boundSql.getParameterObject();
            StringBuffer sb = new StringBuffer();
            List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
            if (parameterMappings.size() > 0 && parameterObject != null) {
                if (parameterMappings != null && parameterMappings.size() == 1
                        && parameterObject != null && parameterObject instanceof String) {
                    Object encode = encode(parameterObject + "");
                    setFieldValue(parameterHandler, "parameterObject", encode);
                    sb.append("(").append(parameterObject).append(" -> ").append(encode).append("),");
                } else {
                    MetaObject metaObject = configuration.newMetaObject(parameterObject);
                    if (parameterObject instanceof DefaultSqlSession.StrictMap) {       //表示一个集合
                        Set<String> keySet = ((DefaultSqlSession.StrictMap<?>) parameterObject).keySet();
                        if(keySet.contains("list")){
                            List<Object> list = (List<Object>) metaObject.getValue("list");
                            if (list != null && list.size() > 0) {
                                Object object = list.get(0);
                                if(object instanceof  String){
                                    setValueOrSetAdditionalParameter(paramMaps,parameterMappings,boundSql,metaObject ,sb );
                                }else{
                                    List<Object> collection = (List<Object>) metaObject.getValue("collection");
                                    int i = 0 ;
                                    for (Map.Entry<Integer, String> paramMap : paramMaps.entrySet()) {
                                        MetaObject meta1 = configuration.newMetaObject( list.get(i));
                                        MetaObject meta2 = configuration.newMetaObject( collection.get(i));
                                        String propertyName = PSqlParseUtil.field2JavaCode(paramMap.getValue());
                                        Object temp = meta1.getValue(propertyName);
                                        Object value = encode(temp);
                                        meta1.setValue(propertyName, value);
                                        meta2.setValue(propertyName, value);
                                        i ++;
                                        sb.append("(").append(propertyName).append(" : ").append(temp).append(" -> ").append(value).append("),");
                                    }

                                }
                            }
                        }else{
                            Object[] array = (Object[]) metaObject.getValue("array");
                            if (array != null && array.length > 0) {
                                Object object = array[0];
                                if(object instanceof  String){
                                    setValueOrSetAdditionalParameter(paramMaps,parameterMappings,boundSql,metaObject ,sb );
                                }else{
                                    int i = 0 ;
                                    for (Map.Entry<Integer, String> paramMap : paramMaps.entrySet()) {
                                        MetaObject meta1 = configuration.newMetaObject(array[i]);
                                        String propertyName = PSqlParseUtil.field2JavaCode(paramMap.getValue());
                                        Object temp = meta1.getValue(propertyName);
                                        Object value = encode(temp);
                                        meta1.setValue(propertyName, value);
                                        i ++;
                                        sb.append("(").append(propertyName).append(" : ").append(temp).append(" -> ").append(value).append("),");
                                    }
                                }
                            }
                        }
                    } else {
                        setValueOrSetAdditionalParameter(paramMaps,parameterMappings,boundSql,metaObject ,sb );
                    }
                }
            }
            String a = sb.toString();
        } catch (Exception e) {
            try {
                PDingDingUtils.sendText("异常编号 =" + Logger.inheritableThreadLocalNo.get() + "\n key =" + key + "\n mapperId =" + mapperdId + "\n sql = " + boundSql.getSql() + "\n 异常堆栈 = " + PExceptionUtils.dealException(e));
                PPTuple pluginTuple = enterMap.get(key);
                log.error("sql insert/update/select/delete 加密异常 exception sql = " + sql + ", key = " + key + " ,mappid = " + mapperdId + " ,data =   " + JSON.toJSONString(data) + ", pluginTuple = " + JSON.toJSONString(pluginTuple), e);
            } catch (Exception ex) {
                log.error("异常xxx", e);
            }
        }
    }

    public void setValueOrSetAdditionalParameter(Map<Integer, String> paramMaps, List<ParameterMapping> parameterMappings,
                                                 BoundSql boundSql, MetaObject metaObject, StringBuffer sb) {
        for (Map.Entry<Integer, String> paramMap : paramMaps.entrySet()) {
            int index = paramMap.getKey();
            ParameterMapping parameterMapping = parameterMappings.get(index);
            String propertyName = parameterMapping.getProperty();
            PTuple2<Boolean, Object> indetValue = getIndexObject(boundSql, metaObject, propertyName, index).getData();
            Object encode = encode(indetValue.getSecond());
            metaObject.setValue(propertyName, encode);
            sb.append("(").append(propertyName).append(" : ").append(indetValue.getSecond()).append(" -> ").append(encode).append("),");
            if (indetValue.getFirst()) {
                boundSql.setAdditionalParameter(propertyName, encode);
            }
        }
    }

    public static Object encode(Object indetValue) {
        if (indetValue instanceof String) {
            if (indetValue == null || PStringUtils.isEmpty(indetValue.toString())) {
                return indetValue;
            }

            return PAesUtil.encrypt((String) indetValue);
        }
        return indetValue;
    }

    public static <T> T setFieldValue(Object target, String name, Object value) {
        Field field = getField(target.getClass(), name);
        if (field != null) {
            try {
                //修改 final 修饰的变量
                Field modifiersField = Field.class.getDeclaredField("modifiers"); //①
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL); //②

                field.setAccessible(true);
                field.set(target, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    public static Field getField(Class clazz, String name) {
        Field fields[] = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (name.equals(field.getName())) {
                return field;
            }
        }
        return null;
    }


    public PPTuple getChangeColumn(Configuration configuration, String mapperdId, String sql, String key) {
        PPTuple pluginTuple = enterMap.get(key);
        if (pluginTuple != null) {
            return pluginTuple;
        }

        log.info(" 没有从缓存中获取数据,直接生成数据,key= " + key + ", sql = " + sql);
        if ("?".equals(sql.trim())) {
            pluginTuple = new PPTuple(false, null);
            enterMap.put(key, pluginTuple);
            log.info("sql是问号 : " + mapperdId + "sql  : " + sql + ",key = " + key + ",pluginTuple = " + JSON.toJSONString(pluginTuple));
            return pluginTuple;
        }

        Map<Integer, String> linkedHashMap = new LinkedHashMap<>();
        PTuple2<Collection<TableStat.Column>, List<String>> data = PSqlParseUtil.getParserInfo(sql).getData();
        Collection<TableStat.Column> columns = data.getFirst();
        if (columns != null) {
            int count = PStringUtils.getStringCountKey(sql, "?");
            String message = " 解析sql得到的个?数为 " + columns.size() + "，sql中的?个数为 " + count;
            if (count != columns.size() && PStringUtils.getStringCountKey(sql.toLowerCase(), "in") <= 0) {
                log.info("message = " + message + " ,mappId = " + mapperdId + ", sql = " + sql + ",key=" + key);
            }
        }
        List<String> tableNames = data.getSecond();
        Map<String, PPTuple> tableConfig = EncryptTableConfig.tableConfig;
        int i = 0;
        boolean canCache = true;
        for (TableStat.Column column : columns) {
            String tableName = PSqlParseUtil.getRealName(column.getTable());
            String columnName = PSqlParseUtil.getRealName(column.getName());
            if (tableConfig.containsKey(tableName)) {
                if (PSqlParseUtil.notEqQuestionMark(columnName)) {       //
                    PTuple2<List<String>, List<String>> columnNameData = tableConfig.get(tableName).getData();
                    List<String> columnNames = columnNameData.getFirst();
                    if (columnNames.contains(columnName)) {
                        linkedHashMap.put(i, columnName);
                    }
                }
            } else if (PSqlParseUtil.isUnkown(tableName) && PSqlParseUtil.notEqQuestionMark(columnName)) { //如果表名为UNKOWN，但column 不为空时
                int flag = 0;
                for (String realTableName : tableNames) {
                    if (tableConfig.containsKey(realTableName)) {
                        PTuple2<List<String>, List<String>> tableInfos = getTableInfo(configuration, realTableName).getData();
                        List<String> tableColumns = tableInfos.getSecond();
                        PTuple2<List<String>, List<String>> columnNameData = tableConfig.get(realTableName).getData();
                        List<String> columnNames = columnNameData.getFirst();
                        for (String tableColumn : columnNames) {
                            if (tableColumns.contains(tableColumn) && tableColumn.equals(columnName)) {
                                flag++;
                            }
                        }
                    }
                }
                if (flag == 1) {
                    linkedHashMap.put(i, columnName);
                } else {
                    canCache = false;
                }
            }
            i++;
        }
        boolean isEncrypt = linkedHashMap.size() > 0;
        pluginTuple = new PPTuple(linkedHashMap.size() > 0, linkedHashMap);
        if (canCache) {
            enterMap.put(key, pluginTuple);
                log.info(" 加密信息获取 mapperdId : " + mapperdId + " , isEncrypt : " + isEncrypt + ",columns :" + JSON.toJSONString(pluginTuple) + "，key=" + key);
        } else {
            log.info(" 不加入缓存加密数据 mapperdId : " + mapperdId + " , isEncrypt : " + isEncrypt + ",columns :" + JSON.toJSONString(pluginTuple) + "，key=" + key);
        }
        return pluginTuple;
    }


    private static JdbcTemplate jdbcTemplate = null;

    public PPTuple getTableInfo(Configuration configuration, String tableName) {
        if (jdbcTemplate == null) {
            log.info("创建 jdbcTemplate ");
            final Environment environment = configuration.getEnvironment();
            DataSource dataSource = environment.getDataSource();
            jdbcTemplate = new JdbcTemplate(dataSource);
        }
        String sql = "SELECT COLUMN_NAME columnName, DATA_TYPE dataType, COLUMN_COMMENT columnComment,COLUMN_KEY columnKey FROM INFORMATION_SCHEMA.COLUMNS WHERE table_name = '" + tableName + "'";
        List<TableInfo> tableInfos = jdbcTemplate.query(sql, new TableInfoRowMapper());
        List<String> tableColumns = new ArrayList<>();
        List<String> primaryColumns = new ArrayList<>();
        if (tableInfos != null && tableInfos.size() > 0) {
            for (TableInfo tableInfo : tableInfos) {
                tableColumns.add(tableInfo.getColumnName());
                if ("PRI".equals(tableInfo.getColumnKey())) {           // 获取主键
                    primaryColumns.add(tableInfo.getColumnName());
                }
            }
        }
        return new PPTuple(primaryColumns, tableColumns);
    }

    public static PPTuple getIndexObject(BoundSql boundSql, MetaObject metaObject, String propertyName, int index) {
        Object obj = null;
        boolean flag = false;
        if (metaObject.hasGetter(propertyName)) {
            obj = metaObject.getValue(propertyName);
        } else if (boundSql.hasAdditionalParameter(propertyName)) {
            obj = boundSql.getAdditionalParameter(propertyName);
            flag = true;
        }
        return new PPTuple(flag, obj);
    }


    public static Object decode(Object indetValue) {
        if (indetValue instanceof String) {
            if (indetValue == null || PStringUtils.isEmpty(indetValue.toString())) {
                return indetValue;
            }

            return PAesUtil.decrypt((String) indetValue);
        }
        return indetValue;
    }


    public void decodeData(Configuration configuration, String mapperdId, BoundSql boundSql) {
        PTuple2<Boolean, Map<Integer, String>> data = null;
        String sql = boundSql.getSql();
        String key = mapperdId + PMD5Util.encode(sql);
        try {
            PPTuple pluginTuple = enterMap.get(key);
            if (pluginTuple == null) {
                data = getChangeColumn(configuration, mapperdId, sql, key).getData();
            } else {
                data = pluginTuple.getData();
            }
            if (!data.getFirst()) {
                return;
            }
            Map<Integer, String> paramMaps = data.getSecond();
            Object parameterObject = boundSql.getParameterObject();
            List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
            if (parameterMappings.size() > 0 && parameterObject != null) {
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                if (parameterObject instanceof DefaultSqlSession.StrictMap) {       //表示一个集合
                    Set<String> keySet = ((DefaultSqlSession.StrictMap<?>) parameterObject).keySet();
                    if(keySet.contains("list")){
                        List<Object> list = (List<Object>) metaObject.getValue("list");
                        if (list != null && list.size() > 0) {
                            Object object = list.get(0);
                            if (!(object instanceof String)) {
                                List<Object> collection = (List<Object>) metaObject.getValue("collection");
                                int i = 0 ;
                                for (Map.Entry<Integer, String> paramMap : paramMaps.entrySet()) {
                                    MetaObject meta1 = configuration.newMetaObject(list.get(i));
                                    MetaObject meta2 = configuration.newMetaObject(collection.get(i));
                                    String propertyName = PSqlParseUtil.field2JavaCode(paramMap.getValue());
                                    Object temp = meta1.getValue(propertyName);
                                    Object value = decode(temp);
                                    meta1.setValue(propertyName, value);
                                    meta2.setValue(propertyName, value);
                                    i ++;
                                }
                            }
                        }
                    } else {

                        Object[] array = (Object[]) metaObject.getValue("array");
                        if (array != null && array.length > 0) {
                            Object object = array[0];
                            if (!(object instanceof String)) {
                                int i = 0 ;
                                for (Map.Entry<Integer, String> paramMap : paramMaps.entrySet()) {
                                    MetaObject meta1 = configuration.newMetaObject(array[i]);
                                    String propertyName = PSqlParseUtil.field2JavaCode(paramMap.getValue());
                                    Object temp = meta1.getValue(propertyName);
                                    Object value = decode(temp);
                                    meta1.setValue(propertyName, value);
                                    i ++;
                                }
                            }
                        }

                    }
                } else {
                    for (Map.Entry<Integer, String> paramMap : paramMaps.entrySet()) {
                        int index = paramMap.getKey();
                        ParameterMapping parameterMapping = parameterMappings.get(index);
                        String propertyName = parameterMapping.getProperty();
                        PTuple2<Boolean, Object> indetValue = getIndexObject(boundSql, metaObject, propertyName, index).getData();
                        Object decode = decode(indetValue.getSecond());
                        if (decode != null) {
                            metaObject.setValue(propertyName, decode);
                        }
                        if (indetValue.getFirst()) {
                            boundSql.setAdditionalParameter(propertyName, decode);
                        }
                    }
                }
            }
        } catch (Exception e) {
            try {
                log.error("showSql exception " + JSON.toJSONString(data) + ",key = " + key + ",sql = " + sql + ",mapperId" + mapperdId, e);
            } catch (Exception ex) {
                log.error("decodeData exception", e);
            }
        }
    }

}
