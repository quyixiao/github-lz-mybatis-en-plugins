package com.test;

import com.lz.druid.sql.ast.SQLObjectImpl;
import com.lz.druid.sql.ast.SQLStatement;
import com.lz.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.lz.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import com.lz.druid.sql.parser.Lexer;
import com.lz.druid.sql.parser.SQLExprParser;
import com.lz.druid.sql.parser.SQLStatementParser;
import com.lz.druid.stat.TableStat;
import table.Cell;
import util.ConsoleTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TestSql2 {

    public static void printSql(String sql) {
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

        List<Cell> header = new ArrayList<Cell>() {{
            add(new Cell("tableName"));
            add(new Cell("column"));
            add(new Cell("select"));
            add(new Cell("where"));
            add(new Cell("join"));
            add(new Cell("groupBy"));
            add(new Cell("having"));
            add(new Cell("fullName"));
            add(new Cell("dataType"));
            add(new Cell("pos"));
            add(new Cell("test"));
        }};
        List<List<Cell>> body = new ArrayList<List<Cell>>();
        for (TableStat.Column column : list) {
            List<Cell> cells = new ArrayList<Cell>();
            cells.add(new Cell(column.getTable()));
            cells.add(new Cell(column.getName()));
            cells.add(new Cell(column.isSelect() + ""));
            cells.add(new Cell(column.isWhere() + ""));
            cells.add(new Cell(column.isJoin() + ""));
            cells.add(new Cell(column.isGroupBy() + ""));
            cells.add(new Cell(column.isHaving() + ""));
            cells.add(new Cell(column.getFullName() + ""));
            cells.add(new Cell(column.getDataType() + ""));
            cells.add(new Cell(column.getPos() + ""));
            if (column.getPos() > 10) {
                cells.add(new Cell(sql.substring(column.getPos(), column.getPos() + column.getName().length())));
            } else {
                cells.add(new Cell(""));
            }
            body.add(cells);
        }
        String a = new ConsoleTable.ConsoleTableBuilder().addHeaders(header).addRows(body).build().toString();
        System.out.println(a);
    }


    @org.junit.Test
    public void test() {
        String sql = " select * from lt_user_phone up left join lt_borrow b on up.id = b.user_id  " +
                " where up.user_name_en = ? and status = ? ";
        System.out.println(sql);
        printSql(sql);
    }

}
