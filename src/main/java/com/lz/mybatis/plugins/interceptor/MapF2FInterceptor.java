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
import com.lz.mybatis.plugins.interceptor.annotation.Bean2Map;
import com.lz.mybatis.plugins.interceptor.annotation.Pull;
import com.lz.mybatis.plugins.interceptor.baomidou.entity.PullInfo;
import com.lz.mybatis.plugins.interceptor.baomidou.toolkit.PluginUtils;
import com.lz.mybatis.plugins.interceptor.utils.PDingDingUtils;
import com.lz.mybatis.plugins.interceptor.utils.PExceptionUtils;
import com.lz.mybatis.plugins.interceptor.utils.PSqlParseUtil;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.core.DefaultParameterNameDiscoverer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
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
public class MapF2FInterceptor implements Interceptor {

    private static final List<Class<?>> primitiveTypes = new ArrayList<>(32);
    public final static String TABLENAME = "TableName";

    static {
        primitiveTypes.add(Boolean.class);
        primitiveTypes.add(Byte.class);
        primitiveTypes.add(Character.class);
        primitiveTypes.add(Double.class);
        primitiveTypes.add(Float.class);
        primitiveTypes.add(Integer.class);
        primitiveTypes.add(Long.class);
        primitiveTypes.add(Short.class);
        primitiveTypes.add(BigDecimal.class);

        primitiveTypes.add(boolean.class);
        primitiveTypes.add(byte.class);
        primitiveTypes.add(char.class);
        primitiveTypes.add(double.class);
        primitiveTypes.add(float.class);
        primitiveTypes.add(int.class);
        primitiveTypes.add(long.class);
        primitiveTypes.add(short.class);
        primitiveTypes.add(String.class);
        primitiveTypes.add(Date.class);
        primitiveTypes.add(java.sql.Date.class);

        primitiveTypes.addAll(Arrays.asList(new Class<?>[]{
                boolean[].class, byte[].class, char[].class, double[].class,
                float[].class, int[].class, long[].class, short[].class}));

        primitiveTypes.addAll(Arrays.asList(new Class<?>[]{
                Boolean[].class, Byte[].class, Character[].class, Double[].class,
                Float[].class, Integer[].class, Long[].class, Short[].class, String[].class}));
    }

    private static boolean isBasicDataTypes(Class clazz) {
        return primitiveTypes.contains(clazz) ? true : false;
    }

    private static final Map<String, Pair<Boolean, List<PullInfo>>> fieldMap = new ConcurrentHashMap<>();

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
        // 当前类
        String className = substringBeforeLast(mappedStatement.getId());
        // 当前方法
        String currentMethodName = substringAfterLast(mappedStatement.getId());
        // 获取当前Method
        Method currentMethod = findMethod(className, currentMethodName);
        Object result = invocation.proceed();
        if (currentMethod == null) {
            return result;
        }
        pullProperty(result, currentMethod, mappedStatement, metaObject, invocation);
        // 如果当前Method没有注解MapF2F
        if (currentMethod.getAnnotation(Bean2Map.class) == null) {
            return result;
        }
        // 如果有MapF2F注解，则这里对结果进行拦截并转换
        Bean2Map mapF2FAnnotation = currentMethod.getAnnotation(Bean2Map.class);
        String mapperdId = PSqlParseUtil.getMapperId(mappedStatement);
        BoundSql boundSql = (BoundSql) metaObject.getValue("boundSql");
        Class c = getActualType(currentMethod, 1);
        try {
            if (result != null) {
                Map<Object, Object> map = new HashMap<>();
                if (mapF2FAnnotation.fillNull()) {
                    try {
                        DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
                        String[] parameterNames = parameterNameDiscoverer.getParameterNames(currentMethod);
                        Pair<Integer, String> pair = getMethodParameterInfoByAnnotation(currentMethod);
                        if (pair != null) {
                            String paramName = parameterNames[pair.getKey()];
                            MapperMethod.ParamMap parameterObject = (MapperMethod.ParamMap) metaObject.getValue("parameterHandler.parameterObject");
                            Object object = parameterObject.get(paramName);
                            if (object instanceof Collection) {
                                forceFillList(map, (Collection) object, pair.getValue(), c);
                            } else if (object.getClass().isArray()) {     //如果是数组类型
                                forceFillList(map, Arrays.asList((Object[]) object), pair.getValue(), c);
                            } else {
                                forceFillObject(map, object, pair.getValue(), c);
                            }
                        }
                    } catch (Exception e) {
                        log.error("解析参数异常", e);
                    }
                }
                if (result instanceof Collection) {
                    forceChageList(map, (Collection) result, mapF2FAnnotation, c, currentMethod);
                } else if (result.getClass().isArray()) {     //如果是数组类型
                    forceChageList(map, Arrays.asList((Object[]) result), mapF2FAnnotation, c, currentMethod);
                } else {
                    forceChange(map, result, mapF2FAnnotation, c, currentMethod);
                }
                List<Object> list = new ArrayList<>();
                list.add(map);
                return list;
            }

        } catch (Exception e) {
            log.error("MapF2FInterceptor exception ", e);
            PDingDingUtils.sendText("异常编号 =" + Logger.inheritableThreadLocalNo.get()
                    + "\n mapperId =" + mapperdId
                    + "\n sql = " + boundSql.getSql()
                    + "\n 异常堆栈 = " + PExceptionUtils.dealException(e));
        }
        return result;
    }

    public void pullProperty(Object result, Method currentMethod, MappedStatement mappedStatement,
                             MetaObject metaObject, Invocation invocation) {
        // 获取方法的返回值类型
        Class returnType = getMethodReturnType(currentMethod);
        // 获取返回值的类型
        if (returnType != null) {
            String mappId = mappedStatement.getId();
            Pair<Boolean, List<PullInfo>> fieldPair = fieldMap.get(mappId);
            if (fieldPair != null && !fieldPair.getKey()) {
                return;
            }
            if (fieldPair == null) {
                List<PullInfo> hashPull = getPullInfo(returnType);
                if (hashPull.size() > 0) {
                    fieldPair = new Pair<>(true, hashPull);
                    fieldMap.put(mappId, fieldPair);
                } else {
                    fieldMap.put(mappId, new Pair<>(false, null));
                    return;
                }
            }
            TypeHandlerRegistry typeHandlerRegistry = (TypeHandlerRegistry) metaObject.getValue("mappedStatement.configuration.typeHandlerRegistry");
            ObjectFactory objectFactory = (ObjectFactory) metaObject.getValue("mappedStatement.configuration.objectFactory");
            //获取当前resutType的类型
            Object object = QueryDecryptScopeInterceptor.realTarget(invocation.getArgs()[0]);
            Connection connection = null;
            if (object != null) {
                MetaObject metaObjectResult = SystemMetaObject.forObject(object);
                if ("JDBC42PreparedStatement".equals(object.getClass().getSimpleName())) {
                    connection = (Connection) metaObjectResult.getValue("connection");
                } else if ("DruidPooledPreparedStatement".equals(object.getClass().getSimpleName())) {
                    connection = (Connection) metaObjectResult.getValue("conn");
                }
                if (connection != null) {
                    //BoundSql对象是处理SQL语句用的
                    pullData(result, connection, fieldPair.getValue(), typeHandlerRegistry, objectFactory);
                }
            }
        }
    }

    public Class getMethodReturnType(Method currentMethod) {
        Class returnType = null;
        if (currentMethod != null && currentMethod.getAnnotation(Bean2Map.class) != null) {
            // 返回值是Map<String,User> 的情况
            returnType = getActualType(currentMethod, 1);
            if (returnType == ArrayList.class) {
                // 返回值是Map<String,List<User> 的情况
                returnType = getActualType(currentMethod, 1, 0);
            }
        } else {
            returnType = currentMethod.getReturnType();
            if (isList(returnType)) {
                returnType = getActualType(currentMethod, 0);
            } else if (isMap(returnType)) {
                returnType = null;
            }
        }
        return returnType;
    }

    public void pullData(Object result, Connection connection, List<PullInfo> pullInfos,
                         TypeHandlerRegistry typeHandlerRegistry, ObjectFactory objectFactory) {
        //BoundSql对象是处理SQL语句用的
        if (result instanceof Collection) {
            fillPropertyList((Collection) result, connection, pullInfos, typeHandlerRegistry, objectFactory);
        } else if (result.getClass().isArray()) {     //如果是数组类型
            fillPropertyList(Arrays.asList((Object[]) result), connection, pullInfos, typeHandlerRegistry, objectFactory);
        } else {
            fillProperty(result, connection, pullInfos, typeHandlerRegistry, objectFactory);
        }
    }

    public void fillPropertyList(Collection collection, Connection connection, List<PullInfo> fields,
                                 TypeHandlerRegistry typeHandlerRegistry, ObjectFactory objectFactory) {
        Iterator iterator = collection.iterator();
        //hasNext():判断是否还下一个元素
        while (iterator.hasNext()) {
            Object o = iterator.next();
            fillProperty(o, connection, fields, typeHandlerRegistry, objectFactory);
        }
    }

    public void fillProperty(Object o, Connection connection, List<PullInfo> fields, TypeHandlerRegistry typeHandlerRegistry,
                             ObjectFactory objectFactory) {
        MetaObject metaO = SystemMetaObject.forObject(o);
        for (PullInfo pullInfo : fields) {
            String sql = combineSql(pullInfo, metaO, pullInfo.isList());
            log.info(" Pull sql = " + sql);
            Object obj = getObject(sql, connection, pullInfo.isList(), pullInfo.getFieldType(), typeHandlerRegistry, objectFactory);
            try {
                Method method = o.getClass().getMethod("set" + upperCaseFirst(pullInfo.getFieldName()), pullInfo.getOriginType());
                method.setAccessible(true);
                method.invoke(o, obj);
                // 如果是非基本数据类型，或是list 集合
                if (obj != null && (pullInfo.isList() || !isBasicDataTypes(pullInfo.getFieldType()))) {
                    List<PullInfo> hashPull = getPullInfo(pullInfo.getFieldType());
                    if (hashPull != null && hashPull.size() > 0) {
                        pullData(obj, connection, hashPull, typeHandlerRegistry, objectFactory);
                    }
                }
            } catch (Exception e) {
                log.error("fillProperty 异常", e);
            }
        }
    }

    public String combineSql(PullInfo pullInfo, MetaObject metaO, boolean isList) {
        StringBuilder sb = new StringBuilder("select * from " + pullInfo.getTableName() + " where IS_DELETE = 0 ");
        boolean flag = true;
        if (pullInfo.getSelf() != null && !"".equals(pullInfo.getSelf())
                && pullInfo.getTarget() != null && !"".equals(pullInfo.getTarget())) {
            flag = false;
            Object selfValue = getCanableValue(metaO, pullInfo.getSelf());
            sb.append(" AND ").append(getDataBaseColumn(pullInfo.getTarget())).append(" = ");
            if (selfValue instanceof String) {
                sb.append("'").append(selfValue).append("'");
            } else {
                sb.append(selfValue);
            }
        }
        // 如果where 不为空
        if (pullInfo.getWhere() != null && !"".equals(pullInfo.getWhere())) {
            flag = false;
            char wheres[] = pullInfo.getWhere().toCharArray();
            if (!pullInfo.getWhere().toUpperCase().trim().startsWith("AND")) {
                sb.append(" AND ");
            }
            boolean tempFlag = false;
            StringBuilder temp = new StringBuilder();
            for (int i = 0; i < wheres.length; i++) {
                char c = wheres[i];
                if ("#".equals(c + "")) {
                    tempFlag = true;
                    i++;
                    continue;
                }
                if ("}".equals(c + "")) {
                    Object selfValue = getCanableValue(metaO, temp.toString());
                    if (selfValue instanceof String) {
                        sb.append("'").append(selfValue).append("'");
                    } else {
                        sb.append(selfValue);
                    }
                    tempFlag = false;
                    temp = new StringBuilder();
                    continue;
                }
                if (tempFlag) {
                    temp.append(c);
                } else {
                    sb.append(c);
                }
            }
        }
        if (flag) {
            sb.append(" AND 1 = 0 ");
        }
        String tempSql = sb.toString();
        if (pullInfo.getSort() != null && pullInfo.getSort() != ""
                && !tempSql.toUpperCase().contains("ORDER BY")
                && !tempSql.toUpperCase().contains("LIMIT")
        ) {
            sb.append(" ORDER BY id " + pullInfo.getSort());
        }

        if (!tempSql.toUpperCase().contains("LIMIT")) {
            if (isList) {
                sb.append(" LIMIT ").append(pullInfo.getLimit());
            } else {
                sb.append(" LIMIT 1 ");
            }

        }
        return sb.toString();
    }


    public List<PullInfo> getPullInfo(Class returnType) {
        List<PullInfo> hashPull = new ArrayList<>();
        List<Field> fields = new ArrayList<>();
        getAllField(fields, returnType);
        for (Field field : fields) {
            Pull pull = field.getAnnotation(Pull.class);
            if (pull != null) {
                Class type = field.getType();
                boolean isList = false;
                if (type.getName().startsWith("java.util.List")) {
                    type = getFieldActualType(field, 0);
                    isList = true;
                }
                String tableName = getAnnotationValueByTypeName(type, TABLENAME);
                if (tableName == null) {
                    continue;
                }
                hashPull.add(new PullInfo(pull.self(), pull.target(), pull.sort(), pull.limit(),
                        field.getName(), tableName, pull.where(), type, isList, field.getType()));
            }
        }
        return hashPull;
    }


    protected Object getObject(String sql, Connection connection, boolean isList, Class clazz,
                               TypeHandlerRegistry typeHandlerRegistry, ObjectFactory objectFactory) {
        List<Object> resutList = new ArrayList<>();
        try (java.sql.PreparedStatement statement = connection.prepareStatement(sql)) {
            List<Field> list = new ArrayList<>();
            getAllField(list, clazz);
            try (ResultSet rs = statement.executeQuery()) {
                boolean flag = true;
                while (rs.next()) {
                    flag = false;
                    Object object = objectFactory.create(clazz);
                    for (Field field : list) {
                        TypeHandler<?> typeHandler = typeHandlerRegistry.getTypeHandler(field.getType());
                        if (typeHandler != null) {
                            Object propValue = typeHandler.getResult(rs, getDataBaseColumn(field.getName()));
                            field.setAccessible(true);
                            field.set(object, propValue);
                        }
                    }
                    if (isList) {
                        resutList.add(object);
                    } else {
                        return object;
                    }
                }
                if (flag && !isList) {
                    return null;
                }
            }
        } catch (Exception e) {
            log.error(" getObject 异常", e);
        }
        return resutList;
    }

    // getFields方法 , 返回一个Field类型数组，其中包含当前类的public字段，如果此类继承于某个父类，同事包括父类的public字段。
    // 其它的proteced和private字段，无论是属于当前类还是父类都不被此方法获取。
    // getDeclareFields方法
    // 返回一个Field类型数组，结果包含当前类的所有字段，private、protected、public或者无修饰符都在内。另外，此方法返回的结果不包括父类的任何字段。 此方法只是针对当前类的。
    public void getAllField(List<Field> list, Class returnClass) {
        if (returnClass == Object.class || returnClass == null) {
            return;
        }
        Field[] fields = returnClass.getDeclaredFields();
        for (Field field : fields) {
            list.add(field);
        }
        getAllField(list, returnClass.getSuperclass());
    }

    public static Pair<Integer, String> getMethodParameterInfoByAnnotation(Method method) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        if (parameterAnnotations == null || parameterAnnotations.length == 0) {
            return null;
        }
        int i = 0;
        for (Annotation[] parameterAnnotation : parameterAnnotations) {
            for (Annotation annotation : parameterAnnotation) {
                String annotaionName = getAnnotationName(annotation);
                if ("Row".equals(annotaionName)) {
                    String value = getAnnotationValue(annotation);
                    return new Pair<>(i, value);
                }
            }
            i++;
        }
        return new Pair<>(null, null);
    }

    public static String getAnnotationName(Annotation annotation) {
        String annotionStr = annotation.toString();
        int a = annotionStr.indexOf("(", 0);
        if (a != -1) {
            annotionStr = annotionStr.substring(0, a);
            String strs[] = annotionStr.split("\\.");
            if (strs != null && strs.length > 0) {
                return strs[strs.length - 1];
            }
        }
        return annotionStr;
    }

    public static <T> T getAnnotationValue(Annotation annotation) {
        try {
            Method method = annotation.getClass().getMethod("value");
            if (method != null) {
                T paramName = (T) method.invoke(annotation);
                return paramName;
            }
        } catch (NoSuchMethodException e) {

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static Class getActualType(Method method, int index) {
        // 获取返回值类型，getGenericReturnType()会返回值带有泛型的返回值类型
        Type genericReturnType = method.getGenericReturnType();
        // 但我们实际上需要获取返回值类型中的泛型信息，所以要进一步判断，即判断获取的返回值类型是否是参数化类型ParameterizedType
        try {
            if (genericReturnType instanceof ParameterizedType) {
                // 如果要使用ParameterizedType中的方法，必须先强制向下转型
                ParameterizedType type = (ParameterizedType) genericReturnType;
                // 获取返回值类型中的泛型类型，因为可能有多个泛型类型，所以返回一个数组
                Type[] actualTypeArguments = type.getActualTypeArguments();
                Type types = actualTypeArguments[index];
                if (types.getTypeName().startsWith("java.util.List")) {
                    return ArrayList.class;
                } else if (types.getTypeName().startsWith("java.util.Map")) {
                    return HashMap.class;
                }
                return Class.forName(types.getTypeName());
            }
        } catch (ClassNotFoundException e) {
            log.error("ClassNotFoundException异常", e);
        }

        return null;
    }


    public static boolean isList(Class clz) {
        if (clz.getName().startsWith("java.util.List")) {
            return true;
        }
        return false;
    }


    public static boolean isMap(Class clz) {
        if (clz.getName().startsWith("java.util.Map")) {
            return true;
        }
        return false;
    }

    public static Class getFieldActualType(Field field, int index) {
        Type genericReturnType = field.getGenericType();
        // 但我们实际上需要获取返回值类型中的泛型信息，所以要进一步判断，即判断获取的返回值类型是否是参数化类型ParameterizedType
        try {
            if (genericReturnType instanceof ParameterizedType) {
                // 如果要使用ParameterizedType中的方法，必须先强制向下转型
                ParameterizedType type = (ParameterizedType) genericReturnType;
                // 获取返回值类型中的泛型类型，因为可能有多个泛型类型，所以返回一个数组
                Type[] actualTypeArguments = type.getActualTypeArguments();
                Type types = actualTypeArguments[index];
                return Class.forName(types.getTypeName());
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }


    // 如  ,获取返回参数类型为    Map<Long,List<User>>  类型
    //  获取User类型
    public static Class getActualType(Method method, int index, int index2) {
        // 获取返回值类型，getGenericReturnType()会返回值带有泛型的返回值类型
        Type genericReturnType = method.getGenericReturnType();
        // 但我们实际上需要获取返回值类型中的泛型信息，所以要进一步判断，即判断获取的返回值类型是否是参数化类型ParameterizedType
        try {
            if (genericReturnType instanceof ParameterizedType) {
                // 如果要使用ParameterizedType中的方法，必须先强制向下转型
                ParameterizedType type = (ParameterizedType) genericReturnType;
                // 获取返回值类型中的泛型类型，因为可能有多个泛型类型，所以返回一个数组
                Type[] actualTypeArguments = type.getActualTypeArguments();
                Type types = actualTypeArguments[index];
                ParameterizedType type2 = (ParameterizedType) types;
                Type[] type2s = type2.getActualTypeArguments();
                Type real = type2s[index2];
                return Class.forName(real.getTypeName());
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


    public void forceChageList(Map<Object, Object> map, Collection collection, Bean2Map mapF2FAnnotation, Class c, Method method) {
        Iterator iterator = collection.iterator();
        //hasNext():判断是否还下一个元素
        while (iterator.hasNext()) {
            //next():①指针下移 ②将下移以后集合位置上的元素返回
            Object o = iterator.next();
            forceChange(map, o, mapF2FAnnotation, c, method);
        }
    }

    public void forceFillList(Map<Object, Object> map, Collection collection, String key, Class c) {
        Iterator iterator = collection.iterator();
        //hasNext():判断是否还下一个元素
        while (iterator.hasNext()) {
            //next():①指针下移 ②将下移以后集合位置上的元素返回
            Object o = iterator.next();
            forceFillObject(map, o, key, c);
        }
    }


    public void forceFillObject(Map<Object, Object> map, Object o, String key, Class c) {
        try {
            if (!isBasicDataTypes(o.getClass()) && !isBasicDataTypes(c)) {
                MetaObject metaO = SystemMetaObject.forObject(o);
                Object k = metaO.getValue(key);
                map.put(k, c.newInstance());
            }
        } catch (Exception e) {
            log.error("fill null data  参数异常", e);
        }
    }

    public void forceChange(Map<Object, Object> map, Object o, Bean2Map mapF2FAnnotation, Class c, Method method) {
        MetaObject metaO = SystemMetaObject.forObject(o);
        Object x = null;
        Object v = null;
        try {
            String keys[] = mapF2FAnnotation.key();
            if (keys.length == 1) {
                x = metaO.getValue(getDataBaseColumn(keys[0]));
            } else {
                StringBuilder sb = new StringBuilder();
                for (String key : keys) {
                    Object r = getValue(metaO, getDataBaseColumn(key));
                    sb.append(r == null ? key : r);
                }
                x = sb.toString();
            }
            if ("this".equals(mapF2FAnnotation.value())) {
                if (c == ArrayList.class || c == List.class) {
                    v = map.get(x);
                    if (v == null) {
                        v = new ArrayList<>();
                    }
                    // 如 c  是 List<User> 类型，则获取 User.class
                    Class xx = getActualType(method, 1, 0);
                    Object temp = null;
                    if (o instanceof Map) {
                        temp = toBean(xx, (Map) o);
                    } else {
                        temp = o;
                    }
                    ((List) v).add(temp);
                } else {
                    if (o instanceof Map) {
                        v = toBean(c, (Map) o);
                    } else {
                        v = o;
                    }
                }
            } else {
                String valueK = getDataBaseColumn(mapF2FAnnotation.value());
                v = metaO.getValue(valueK);
            }
        } catch (Exception e) {
            log.error("forceChange 异常", e);
        }
        map.put(x, v);
    }


    public Object getCanableValue(MetaObject metaO, String key) {
        Object b = getValue(metaO, key);
        if (b == null) {
            b = getValue(metaO, getDataBaseColumn(key));
        }
        return b;
    }


    public Object getValue(MetaObject metaO, String key) {
        try {
            return metaO.getValue(key);
        } catch (Exception e) {
        }
        return null;
    }

    public static <E> E toBean(Class<E> cla, Map<String, Object> map) {
        // 创建对象
        E obj = null;
        try {
            obj = cla.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        // 进行值装入
        Method[] ms = cla.getMethods();
        for (Method method : ms) {
            String name = method.getName();
            String mname = name.substring(0, 1).toUpperCase() + name.substring(1); // 将属性的首字符大写，方便构造get，set方法
            if (mname.startsWith("set") || mname.startsWith("Set")) {
                Class[] clas = method.getParameterTypes();
                String a = mname.substring(3, mname.length());
                a = getDataBaseColumn(a);
                Object v = map.get(a);
                if (v != null && clas.length == 1) {
                    try {
                        if (clas[0] == Integer.class && v instanceof Boolean) {
                            if ((Boolean) v) {
                                v = 1;
                            } else {
                                v = 0;
                            }
                        }
                        method.invoke(obj, v);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return obj;
    }


    public static String upperCaseFirst(String v) {
        String a = v.substring(0, 1).toUpperCase();
        String b = v.substring(1);
        return a + b;
    }

    public static boolean isUpperCase(char c) {
        if (c >= 'A' && c <= 'Z') {
            return true;
        }
        return false;
    }

    public static String getDataBaseColumn(String javaName) {
        StringBuilder sb = new StringBuilder();
        char[] javaNames = javaName.toCharArray();
        int i = 0;
        for (char c : javaNames) {
            if (i != 0 && isUpperCase(c)) {
                sb.append("_");
            }
            sb.append((c + "").toLowerCase());
            i++;
        }
        return sb.toString();
    }


    public static String substringBeforeLast(String mappenrdId) {
        int i = mappenrdId.lastIndexOf(".");
        return mappenrdId.substring(0, i);
    }


    public static String substringAfterLast(String mappenrdId) {
        int i = mappenrdId.lastIndexOf(".");
        return mappenrdId.substring(i + 1);
    }

    /**
     * 找到与指定函数名匹配的Method。
     *
     * @param className
     * @param targetMethodName
     * @return
     * @throws Throwable
     */
    private Method findMethod(String className, String targetMethodName) throws Throwable {
        // 该类所有声明的方法
        Method[] methods = Class.forName(className).getDeclaredMethods();
        if (methods == null) {
            return null;
        }


        for (Method method : methods) {
            if (method.getName().equals(targetMethodName)) {
                return method;
            }
        }


        return null;
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

    // 递归获取@TableName 注解的值
    public static <T> T getAnnotationValueByTypeName(Class type, String name) {
        if (type == Object.class) {
            return null;
        }
        Annotation[] annotations = type.getAnnotations();
        if (annotations != null && annotations.length > 0) {
            for (Annotation annotation : annotations) {
                if (name.equals(getAnnotationName(annotation))) {
                    return getAnnotationValue(annotation);
                }
            }
        }
        return getAnnotationValueByTypeName(type.getSuperclass(), name);
    }

}
