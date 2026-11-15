package tn.enicarthage.onemorebite.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    private static final String URL = System.getenv().getOrDefault(
            "DB_URL",
            "jdbc:mysql://localhost:3306/onemorebite_db"
                    + "?sslMode=DISABLED"
                    + "&serverTimezone=UTC"
                    + "&allowMultiQueries=true"
    );

    private static final String USER = System.getenv().getOrDefault(
            "DB_USERNAME", "root"
    );

    private static final String PASSWORD = System.getenv().getOrDefault(
            "DB_PASSWORD", ""
    );

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}