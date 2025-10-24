import java.sql.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DatabaseHelper {

    private String dbUrl = "jdbc:sqlite:news_cache.db";

    public DatabaseHelper() {
        createNewTables();
    }

    private void createNewTables() {
        String sqlCache = "CREATE TABLE IF NOT EXISTS news_cache ( id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT NOT NULL UNIQUE, json_data TEXT NOT NULL, fetched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);";
        String sqlSaved = "CREATE TABLE IF NOT EXISTS saved_articles ( id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, description TEXT NOT NULL, url TEXT NOT NULL, saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, UNIQUE(title, description));";
        String sqlUsers = "CREATE TABLE IF NOT EXISTS users ( id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, password_hash TEXT NOT NULL);";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlCache);
            stmt.execute(sqlSaved);
            stmt.execute(sqlUsers);
        } catch (SQLException e) {
            System.out.println("Error creating tables: " + e.getMessage());
        }
    }

    public boolean registerUser(String username, String passwordHash) {
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE")) {
                System.out.println("Registration failed: Username already taken.");
            } else {
                System.out.println("Error registering user: " + e.getMessage());
            }
            return false;
        }
    }
    public String getPasswordHashForUser(String username) {
        String sql = "SELECT password_hash FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getString("password_hash") : null;
        } catch (SQLException e) {
            System.out.println("Error checking user: " + e.getMessage());
            return null;
        }
    }

    // --- (saveNews and getRecentNews are unchanged) ---
    public void saveNews(String category, String json) {
        String sql = "INSERT OR REPLACE INTO news_cache (category, json_data, fetched_at) VALUES (?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category);
            pstmt.setString(2, json);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error saving news to cache: " + e.getMessage());
        }
    }
    public String getRecentNews(String category) {
        String sql = "SELECT json_data FROM news_cache WHERE category = ? AND fetched_at > datetime('now', '-10 minute')";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getString("json_data") : null;
        } catch (SQLException e) {
            System.out.println("Error getting recent news: " + e.getMessage());
            return null;
        }
    }

    // --- REMOVED ---
    // The old saveArticlesFromJson method is gone.

    // --- NEW ---
    // Saves a single, user-selected article to the database.
    // Returns true if a new row was added, false if it was a duplicate.
    public boolean saveSingleArticle(String title, String description, String url) {
        String sql = "INSERT OR IGNORE INTO saved_articles (title, description, url) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, title);
            pstmt.setString(2, description);
            pstmt.setString(3, url);

            int rowsAffected = pstmt.executeUpdate();
            return (rowsAffected > 0); // True if a new article was saved

        } catch (SQLException e) {
            System.out.println("Error saving single article: " + e.getMessage());
            return false;
        }
    }
    public String getSavedArticlesAsHtml() {
        StringBuilder newsHtml = new StringBuilder("<html><body>");
        String sql = "SELECT title, description, url FROM saved_articles ORDER BY id DESC";
        int count = 0;
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String title = rs.getString("title");
                String description = rs.getString("description");
                String url = rs.getString("url");

                newsHtml.append("<div class='news-item'>");
                newsHtml.append("<b><a href='" + url + "'>" + title + "</a></b><br>");
                newsHtml.append(description);
                newsHtml.append("</div>");
                count++;
            }
        } catch (SQLException e) {
            System.out.println("Error getting saved articles: " + e.getMessage());
            return "<html><body><div class='prompt'>Error loading saved articles. Check if database file is old.</div></body></html>";
        }
        if (count == 0) {
            return "<html><body><div class='prompt'>You have no saved articles.</div></body></html>";
        }
        newsHtml.append("</body></html>");
        return newsHtml.toString();
    }
}