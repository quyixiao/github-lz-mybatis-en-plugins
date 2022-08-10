package com.lz.mybatis.plugins.interceptor.baomidou.entity;

public class PullInfo {

    private String self;

    private String target;

    private String sort;

    private int limit;

    private String fieldName;

    private String tableName;
    private String where;
    private Class fieldType;
    private boolean isList;
    private Class originType;

    public PullInfo() {

    }

    public PullInfo(String self, String target, String sort, int limit, String fieldName,
                    String tableName, String where, Class fieldType, boolean isList, Class originType) {
        this.self = self;
        this.target = target;
        this.sort = sort;
        this.limit = limit;
        this.fieldName = fieldName;
        this.tableName = tableName;
        this.where = where;
        this.fieldType = fieldType;
        this.isList = isList;
        this.originType = originType;
    }

    public Class getOriginType() {
        return originType;
    }

    public void setOriginType(Class originType) {
        this.originType = originType;
    }

    public String getWhere() {
        return where;
    }

    public void setWhere(String where) {
        this.where = where;
    }

    public Class getFieldType() {
        return fieldType;
    }

    public boolean isList() {
        return isList;
    }

    public void setList(boolean list) {
        isList = list;
    }

    public void setFieldType(Class fieldType) {
        this.fieldType = fieldType;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getSelf() {
        return self;
    }

    public void setSelf(String self) {
        this.self = self;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
