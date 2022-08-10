//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.lz.mybatis.plugins.interceptor.entity;

public class TableInfo {
    private String columnName;
    private String dataType;
    private String columnComment;
    private String columnKey;

    public TableInfo() {
    }

    public TableInfo(String columnName, String dataType, String columnComment, String columnKey) {
        this.columnName = columnName;
        this.dataType = dataType;
        this.columnComment = columnComment;
        this.columnKey = columnKey;
    }

    public String getColumnName() {
        return this.columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getDataType() {
        return this.dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getColumnComment() {
        return this.columnComment;
    }

    public void setColumnComment(String columnComment) {
        this.columnComment = columnComment;
    }

    public String getColumnKey() {
        return this.columnKey;
    }

    public void setColumnKey(String columnKey) {
        this.columnKey = columnKey;
    }
}
