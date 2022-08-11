package com.test;


import com.lz.druid.stat.TableStat;
import com.lz.mybatis.plugins.interceptor.utils.PSqlParseUtil;
import com.lz.mybatis.plugins.interceptor.utils.PStringUtils;
import com.lz.mybatis.plugins.interceptor.utils.t.PTuple2;

import java.util.Collection;
import java.util.List;

public class Test {




    public static void main(String[] args) {
        String sql = " select *   from lt_user_phone where  user_name_en in (?,?,?,?) and real_name_en = ?";
        PTuple2<Collection<TableStat.Column>, List<String>> data = PSqlParseUtil.getParserInfo(sql).getData();
        Collection<TableStat.Column> columns = data.getFirst();
        for (TableStat.Column column : columns) {
            String tableName = PSqlParseUtil.getRealName(column.getTable());
            String columnName = PSqlParseUtil.getRealName(column.getName());
            System.out.println(tableName + " " + columnName);
        }

    }

}
