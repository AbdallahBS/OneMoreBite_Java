package tn.enicarthage.onemorebite.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseConnection {

    private static final String HOST = "localhost";
    private static final int PORT = 3306;
    private static final String DATABASE = "onemorebite_db";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    private static final String OPTIONS =
            "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
                    + "&allowMultiQueries=true";
    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE + OPTIONS;
    private static final String SERVER_URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + OPTIONS;

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void main(String[] args) {

        Path databaseScript = Paths.get(
                "src", "main", "resources" ,"database", "database.sql"
        );

        try {
            initializeDatabase(Files.readString(databaseScript, StandardCharsets.UTF_8));
            System.out.println("DB + tables creees avec succes.");
        } catch (IOException | SQLException e) {
            System.err.println("Erreur d'initialisation de la base : " + e.getMessage());
            System.exit(1);
        }
    }

    private static void initializeDatabase(String databaseSql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(SERVER_URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            String database = "`" + DATABASE.replace("`", "``") + "`";
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + database
                    + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            statement.execute("USE " + database);
            statement.execute(databaseSql);
        }
    }

}