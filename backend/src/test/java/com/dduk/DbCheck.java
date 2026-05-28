package com.dduk;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbCheck {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("=== [DEBUG] START ACTUAL DB COUNT CHECK ===");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver not found: " + e.getMessage());
        }

        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://gateway01.ap-northeast-1.prod.aws.tidbcloud.com:4000/dduk_erp?useSSL=true",
                "3UCLSyyYTzkXiNP.root",
                "G7rPFFkfVMjTRv9c")) {
            
            String[] tables = {"items", "inventories", "stock_movements", "purchase_orders"};
            for (String table : tables) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    if (rs.next()) {
                        System.out.println("[DB_COUNT] " + table + " : " + rs.getInt(1));
                    }
                } catch (Exception ex) {
                    System.out.println("[DB_COUNT] " + table + " failed: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("DB Connection failed: " + e.getMessage());
        }
        System.out.println("=== [DEBUG] END ACTUAL DB COUNT CHECK ===");
        System.out.println("==================================================");
    }
}
