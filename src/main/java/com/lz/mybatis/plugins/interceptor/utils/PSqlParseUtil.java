package com.lz.mybatis.plugins.interceptor.utils;

import com.lz.druid.sql.ast.SQLObjectImpl;
import com.lz.druid.sql.ast.SQLStatement;
import com.lz.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.lz.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import com.lz.druid.sql.parser.Lexer;
import com.lz.druid.sql.parser.SQLExprParser;
import com.lz.druid.sql.parser.SQLStatementParser;
import com.lz.druid.stat.TableStat;
import com.lz.mybatis.plugins.interceptor.utils.t.PPTuple;
import org.apache.ibatis.mapping.MappedStatement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PSqlParseUtil {

    public static PPTuple getParserInfo(String sql) {
        List<String> tableNames = new ArrayList<>();
        // 新建 MySQL Parser
        SQLStatementParser parser = new MySqlStatementParser(sql);
        SQLExprParser exprParser = parser.getExprParser();
        Lexer lexer = exprParser.getLexer();
        SQLObjectImpl.lexer = lexer;

        // 使用Parser解析生成AST，这里SQLStatement就是AST
        SQLStatement statement = parser.parseStatement();
        // 使用visitor来访问AST
        MySqlSchemaStatVisitor visitor = new MySqlSchemaStatVisitor();

        statement.accept(visitor);
        // 从visitor中拿出你所关注的信息
        Collection<TableStat.Column> list = visitor.getColumns();
        Map<TableStat.Name, TableStat> tableStatMap = visitor.getTables();
        for (Map.Entry<TableStat.Name, TableStat> tableStatEntry : tableStatMap.entrySet()) {
            String tableName = tableStatEntry.getKey().getName();
            tableNames.add(getRealName(tableName));
        }
        return new PPTuple(list, tableNames);
    }

    public static String getRealName(String name) {
        if (PStringUtils.isNotEmpty(name) && name.startsWith("`")) {
            return name.substring(1, name.length() - 1);
        }
        return name;
    }

    public static String toUnderlineCase(String camelCaseStr) {
        if (camelCaseStr == null) {
            return null;
        }
        // 将驼峰字符串转换成数组
        char[] charArray = camelCaseStr.toCharArray();
        StringBuffer buffer = new StringBuffer();
        //处理字符串
        for (int i = 0, l = charArray.length; i < l; i++) {
            if (charArray[i] >= 65 && charArray[i] <= 90) {
                buffer.append("_").append(charArray[i] += 32);
            } else {
                buffer.append(charArray[i]);
            }
        }
        return buffer.toString();
    }

    public static void main(String[] args) {
        System.out.println(toUnderlineCase("abcEdEE"));
    }


    public static String field2JavaCode(String field) {
        String javaCode = field;
        javaCode = javaCode.toLowerCase();
        javaCode = javaCode.trim();
        if (javaCode.contains("_")) {
            String[] codes = javaCode.split("_");
            if (codes.length > 1) {
                for (int i = 1; i < codes.length; i++) {
                    codes[i] = (codes[i].substring(0, 1)).toUpperCase()
                            + codes[i].substring(1);
                }
                javaCode = "";
                for (int i = 0; i < codes.length; i++) {
                    javaCode += codes[i];
                }
            }
            return javaCode;

        }
        return field;
    }


    public static boolean notEqQuestionMark(String value) {
        return !"?".equals(value);
    }

    public static boolean isUnkown(String value) {
        return "UNKOWN".equals(value) || "UNKNOWN".endsWith(value);
    }


    public static String getMapperId(MappedStatement mappedStatement) {
        try {
            String id = mappedStatement.getId();
            if (id.contains(".")) {
                String ids[] = id.split("\\.");
                return ids[ids.length - 2] + "." + ids[ids.length - 1];
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
        return "";
    }

}
