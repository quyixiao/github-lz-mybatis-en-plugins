package com.lz.mybatis.plugins.interceptor.mapper;

import com.lz.mybatis.plugins.interceptor.entity.TableInfo;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TableInfoRowMapper implements RowMapper {

    @Override
    public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
        TableInfo tableInfo = new TableInfo(
                rs.getString("columnName"),
                rs.getString("dataType"),
                rs.getString("columnComment"),
                rs.getString("columnKey")
        );
        return tableInfo;
    }
}
