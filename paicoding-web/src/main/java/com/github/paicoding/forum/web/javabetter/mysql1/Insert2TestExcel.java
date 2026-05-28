package com.github.paicoding.forum.web.javabetter.mysql1;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Insert2TestExcel {
    public static void main(String[] args) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        Statement stmt = null;

        // 1. 补全所有关键连接参数
        String url = "jdbc:mysql://localhost:3306/pai_coding"
                + "?useSSL=false"
                + "&rewriteBatchedStatements=true"
                + "&allowPublicKeyRetrieval=true"
                + "&autoReconnect=true"
                + "&serverTimezone=Asia/Shanghai"
                + "&characterEncoding=utf8";
        String user = "root";
        String password = "020495";

        try {
            // 2. 建立连接
            conn = DriverManager.getConnection(url, user, password);
            stmt = conn.createStatement();

            // 3. 关闭自动提交，开启事务（批量插入核心）
            conn.setAutoCommit(false);

            // 4. 修正SQL：补全ON DUPLICATE KEY UPDATE，语法正确
            // 假设request_count表结构：host(varchar), cnt(int), date(date)
            // 唯一索引 uk_unique_id_date 是 (host, date)
            String insertSQL = "INSERT INTO request_count (host, cnt, date) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE cnt = cnt + VALUES(cnt)";
            pstmt = conn.prepareStatement(insertSQL);

            int batchSize = 5000; // 批量大小
            int totalCount = 5000000; // 总数据量500万
            LocalDate baseDate = LocalDate.of(2020, 1, 1);

            // 5. 修正循环逻辑：避免IP+日期重复，彻底解决唯一键冲突
            for (int i = 0; i < totalCount; i++) {
                // 生成不重复的host：127.0.0.1 ~ 127.0.255.255，避免重复
                String host = String.format("127.0.%d.%d", (i / 256) % 256, i % 256);
                // 生成递增的cnt
                int cnt = 100 + i;
                // 生成递增的日期：从2020-01-01开始，每天一条，避免重复
                LocalDate currentDate = baseDate.plusDays(i / 1000); // 每1000条换一天，避免重复

                pstmt.setString(1, host);
                pstmt.setInt(2, cnt);
                pstmt.setDate(3, java.sql.Date.valueOf(currentDate));

                pstmt.addBatch();

                // 每batchSize条执行一次批量
                if ((i + 1) % batchSize == 0) {
                    pstmt.executeBatch();
                    conn.commit();
                    pstmt.clearBatch();
                    System.out.println("已插入 " + (i + 1) + " 条数据");
                }
            }

            // 6. 处理最后一批不足5000条的数据
            pstmt.executeBatch();
            conn.commit();
            System.out.println("✅ 500万条数据插入完成！");

            // 7. 查询验证（可选）
            ResultSet rs2 = stmt.executeQuery("SELECT COUNT(*) AS total FROM request_count");
            if (rs2.next()) {
                System.out.println("Total rows: " + rs2.getInt("total"));
            }

        } catch (Exception e) {
            // 异常回滚
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Exception rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            e.printStackTrace();
        } finally {
            // 8. 完整关闭资源，避免泄漏
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}