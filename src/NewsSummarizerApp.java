import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import com.google.gson.*;
import javax.swing.AbstractButton;
import java.sql.*;
import javax.imageio.ImageIO;
import java.io.File;
import java.awt.Desktop;
import java.net.URISyntaxException;
import java.io.IOException;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class NewsSummarizerApp extends JFrame {

    // --- (Colors and Fonts are unchanged) ---
    private static final Color COLOR_BG_DARK = new Color(44, 62, 80);
    private static final Color COLOR_BG_LIGHT = new Color(236, 240, 241);
    private static final Color COLOR_TEXT_LIGHT = new Color(255, 255, 255);
    private static final Color COLOR_TEXT_DARK = new Color(52, 73, 94);
    private static final Color COLOR_ACCENT_BLUE = new Color(52, 152, 219);
    private static final Color COLOR_ACCENT_BLUE_HOVER = new Color(82, 172, 239);
    private static final Color COLOR_ACCENT_ORANGE = new Color(230, 126, 34); // For new favorite button
    private static final Color COLOR_ACCENT_ORANGE_HOVER = new Color(243, 156, 18);
    private static final Color COLOR_ACCENT_GREEN = new Color(46, 204, 113);
    private static final Color COLOR_ACCENT_GREEN_HOVER = new Color(76, 224, 143);
    private static final Color COLOR_NEWS_ITEM_BG = new Color(255, 255, 255);
    private static final Color COLOR_NEWS_ITEM_BORDER = new Color(220, 220, 220);

    private static final String FONT_FAMILY = "Segoe UI";
    private static final Font FONT_BODY = new Font(FONT_FAMILY, Font.PLAIN, 16);
    private static final Font FONT_BUTTON = new Font(FONT_FAMILY, Font.BOLD, 16);

    private DatabaseHelper dbHelper;
    private Image backgroundImage;

    // --- MODIFIED ---
    // We now store the parsed JsonArray to pick individual articles
    private JsonArray lastFetchedArticlesArray = null;

    // --- (fetchNewsFromAPI is unchanged) ---
    public static String fetchNewsFromAPI(String category) {
        String apiKey = "9cdcbbb7873e491f9b5ef860987808d8";
        String url = "https://newsapi.org/v2/top-headlines?country=us&category=" + category + "&apiKey=" + apiKey;
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) { e.printStackTrace(); return "Error fetching news. Check console and API key."; }
    }

    // --- (parseNews is unchanged) ---
    public static String parseNews(String json) {
        if (json == null || json.startsWith("Error")) return "<html><body>" + json + "</body></html>";
        StringBuilder newsHtml = new StringBuilder("<html><body>");
        try {
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            JsonArray articles = jsonObject.getAsJsonArray("articles");
            if (articles.size() == 0) return "<html><body><div class='prompt'>No articles returned for this category.</div></body></html>";

            for (JsonElement articleElem : articles) {
                JsonObject article = articleElem.getAsJsonObject();
                String title = article.has("title") && !article.get("title").isJsonNull() ? article.get("title").getAsString() : "No Title";
                String description = article.has("description") && !article.get("description").isJsonNull() ? article.get("description").getAsString() : "No Description";
                String url = article.has("url") && !article.get("url").isJsonNull() ? article.get("url").getAsString() : "#";

                newsHtml.append("<div class='news-item'>");
                newsHtml.append("<b><a href='" + url + "'>" + title + "</a></b><br>");
                newsHtml.append(description);
                newsHtml.append("</div>");
            }
            return newsHtml.append("</body></html>").toString();
        } catch (Exception e) { e.printStackTrace(); return "<html><body><div class='prompt'>Error parsing news.</div></body></html>"; }
    }

    // --- (createModernButton is unchanged) ---
    public static JButton createModernButton(String text, Color background, Color hover) {
        JButton button = new JButton(text);
        button.setFont(FONT_BUTTON);
        button.setBackground(background);
        button.setForeground(COLOR_TEXT_LIGHT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(10, 15, 10, 15));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) { button.setBackground(hover); }
            public void mouseExited(MouseEvent evt) { button.setBackground(background); }
        });
        return button;
    }

    // --- Constructor ---
    public NewsSummarizerApp(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;

        try {
            backgroundImage = ImageIO.read(new File("background.jpg"));
        } catch (Exception e) {
            System.out.println("Could not load background.jpg. Using solid color.");
            backgroundImage = null;
        }

        setTitle("Modern News Summarizer");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(COLOR_BG_LIGHT);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // --- MODIFIED ---
        // We now have two horizontal sections in the top panel
        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.setOpaque(false);

        JPanel topControls = new JPanel(new BorderLayout(10, 10));
        topControls.setBackground(new Color(44, 62, 80, 220));
        topControls.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] categories = {
                "Interests", "Technology", "Sports", "Politics",
                "Health", "Business", "Entertainment", "Science"
        };
        JComboBox<String> categoryComboBox = new JComboBox<>(categories);
        categoryComboBox.setFont(FONT_BODY);
        categoryComboBox.setBackground(COLOR_BG_LIGHT);
        categoryComboBox.setForeground(COLOR_BG_DARK);
        categoryComboBox.setPreferredSize(new Dimension(200, 40));

        // --- MODIFIED ---
        // "Save These" button is REMOVED
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setOpaque(false);
        JButton viewSavedButton = createModernButton("View Saved", COLOR_ACCENT_GREEN, COLOR_ACCENT_GREEN_HOVER);
        JButton fetchButton = createModernButton("Fetch News", COLOR_ACCENT_BLUE, COLOR_ACCENT_BLUE_HOVER);
        buttonPanel.add(viewSavedButton);
        buttonPanel.add(fetchButton);

        topControls.add(categoryComboBox, BorderLayout.WEST);
        topControls.add(buttonPanel, BorderLayout.EAST);

        // --- NEW ---
        // This is the new panel for favoriting individual articles
        JPanel favoritePanel = new JPanel(new BorderLayout(10, 10));
        favoritePanel.setBackground(new Color(60, 80, 100, 200)); // Slightly different color
        favoritePanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JComboBox<String> favoriteBox = new JComboBox<>();
        favoriteBox.setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
        favoriteBox.addItem("Select an article to favorite...");

        JButton favoriteButton = createModernButton("Favorite This Article", COLOR_ACCENT_ORANGE, COLOR_ACCENT_ORANGE_HOVER);
        favoriteButton.setFont(new Font(FONT_FAMILY, Font.BOLD, 14)); // Smaller font
        favoriteButton.setMargin(new Insets(5, 10, 5, 10)); // Smaller button

        favoritePanel.add(favoriteBox, BorderLayout.CENTER);
        favoritePanel.add(favoriteButton, BorderLayout.EAST);
        favoritePanel.setVisible(false); // Hide it until news is fetched
        // --- END NEW ---

        topContainer.add(topControls, BorderLayout.NORTH);
        topContainer.add(favoritePanel, BorderLayout.SOUTH);

        JEditorPane newsArea = new JEditorPane();
        newsArea.setEditable(false);
        newsArea.setContentType("text/html");
        newsArea.setOpaque(false);
        newsArea.setBackground(new Color(0,0,0,0));
        newsArea.setMargin(new Insets(10, 10, 10, 10));

        HTMLEditorKit kit = new HTMLEditorKit();
        newsArea.setEditorKit(kit);
        StyleSheet styleSheet = kit.getStyleSheet();

        styleSheet.addRule(".prompt { color: white; font-weight: bold; font-size: 16px; text-align: center; padding-top: 50px; }");
        styleSheet.addRule("body { font-family: " + FONT_FAMILY + "; font-size: 14px; color: " + String.format("#%02x%02x%02x", COLOR_TEXT_DARK.getRed(), COLOR_TEXT_DARK.getGreen(), COLOR_TEXT_DARK.getBlue()) + "; background-color: transparent; }");
        styleSheet.addRule("a { color: " + String.format("#%02x%02x%02x", COLOR_ACCENT_BLUE.getRed(), COLOR_ACCENT_BLUE.getGreen(), COLOR_ACCENT_BLUE.getBlue()) + "; text-decoration: none; }");
        styleSheet.addRule("a:hover { color: " + String.format("#%02x%02x%02x", COLOR_ACCENT_BLUE_HOVER.getRed(), COLOR_ACCENT_BLUE_HOVER.getGreen(), COLOR_ACCENT_BLUE_HOVER.getBlue()) + "; text-decoration: underline; }");
        styleSheet.addRule(".news-item { background-color: " + String.format("#%02x%02x%02x%02x", COLOR_NEWS_ITEM_BG.getRed(), COLOR_NEWS_ITEM_BG.getGreen(), COLOR_NEWS_ITEM_BG.getBlue(), 230) + "; border: 1px solid " + String.format("#%02x%02x%02x", COLOR_NEWS_ITEM_BORDER.getRed(), COLOR_NEWS_ITEM_BORDER.getGreen(), COLOR_NEWS_ITEM_BORDER.getBlue()) + "; border-radius: 10px; padding: 15px; margin: 10px 5px; }");
        newsArea.setText("<html><body><div class='prompt'>Select a category and press 'Fetch News', or 'View Saved' to see your favorites.</div></body></html>");

        newsArea.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (Desktop.isDesktopSupported()) {
                        try {
                            Desktop.getDesktop().browse(e.getURL().toURI());
                        } catch (IOException | URISyntaxException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(newsArea);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        add(mainPanel);
        mainPanel.add(topContainer, BorderLayout.NORTH); // --- MODIFIED ---
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // --- Button Actions ---
        fetchButton.addActionListener(e -> {
            String selectedCategory = (String) categoryComboBox.getSelectedItem();
            if (selectedCategory == null || selectedCategory.equals("Interests")) {
                newsArea.setText("<html><body><div class='prompt'>Please select a category from the dropdown.</div></body></html>");
                return;
            }
            categoryComboBox.setSelectedItem("Interests");
            newsArea.setText("<html><body><div class='prompt'>Checking cache for " + selectedCategory.toLowerCase() + "...</div></body></html>");
            final String category = selectedCategory.toLowerCase();

            new SwingWorker<String, String>() { // --- MODIFIED --- (Returns JSON string)
                @Override
                protected String doInBackground() {
                    String cachedNews = dbHelper.getRecentNews(category);
                    if (cachedNews != null) {
                        return cachedNews; // Return the JSON
                    } else {
                        String json = fetchNewsFromAPI(category);
                        if (json != null && !json.startsWith("Error")) {
                            dbHelper.saveNews(category, json);
                        }
                        return json; // Return the JSON
                    }
                }

                @Override
                protected void done() {
                    try {
                        String json = get(); // Get the JSON
                        newsArea.setText(parseNews(json)); // Parse it for display
                        newsArea.setCaretPosition(0);

                        // --- NEW ---
                        // Now, parse it again to populate the favorites dropdown
                        favoriteBox.removeAllItems();
                        favoriteBox.addItem("Select an article to favorite...");

                        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                        lastFetchedArticlesArray = jsonObject.getAsJsonArray("articles");

                        for (JsonElement articleElem : lastFetchedArticlesArray) {
                            JsonObject article = articleElem.getAsJsonObject();
                            String title = article.get("title").getAsString();
                            favoriteBox.addItem(title);
                        }
                        favoritePanel.setVisible(true); // Show the favorites panel
                        // --- END NEW ---

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        newsArea.setText("<html><body><div class='prompt'>Error.</div></body></html>");
                        favoritePanel.setVisible(false);
                        lastFetchedArticlesArray = null;
                    }
                }
            }.execute();
        });

        // --- REMOVED ---
        // The "saveButton" listener is gone.

        // --- NEW ---
        // Action listener for the new "Favorite This Article" button
        favoriteButton.addActionListener(e -> {
            int selectedIndex = favoriteBox.getSelectedIndex();

            // Check if they selected the prompt ("Select an article...")
            if (selectedIndex <= 0 || lastFetchedArticlesArray == null) {
                JOptionPane.showMessageDialog(this, "Please select an article from the dropdown first.", "No Article Selected", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Get the corresponding article (subtract 1 for the prompt)
            JsonObject article = lastFetchedArticlesArray.get(selectedIndex - 1).getAsJsonObject();

            // Extract the data
            String title = article.has("title") && !article.get("title").isJsonNull() ? article.get("title").getAsString() : "No Title";
            String description = article.has("description") && !article.get("description").isJsonNull() ? article.get("description").getAsString() : "No Description";
            String url = article.has("url") && !article.get("url").isJsonNull() ? article.get("url").getAsString() : "#";

            // Save to database
            if (dbHelper.saveSingleArticle(title, description, url)) {
                JOptionPane.showMessageDialog(this, "Article saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "This article is already in your favorites.", "Already Saved", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        // --- END NEW ---

        viewSavedButton.addActionListener(e -> {
            newsArea.setText("<html><body><div class='prompt'>Loading saved articles...</div></body></html>");
            categoryComboBox.setSelectedItem("Interests");

            // --- NEW ---
            // Hide the favorites panel when viewing saved articles
            favoritePanel.setVisible(false);
            lastFetchedArticlesArray = null;
            // --- END NEW ---

            new SwingWorker<String, Void>() {
                protected String doInBackground() { return dbHelper.getSavedArticlesAsHtml(); }
                protected void done() {
                    try { newsArea.setText(get()); newsArea.setCaretPosition(0); }
                    catch (Exception ex) { ex.printStackTrace(); newsArea.setText("<html><body><div class='prompt'>Error.</div></body></html>"); }
                }
            }.execute();
        });
    }

    // --- (main method is unchanged) ---
    public static void main(String[] args) {
        DatabaseHelper dbHelper = new DatabaseHelper();
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame(dbHelper);
            loginFrame.setVisible(true);
        });
    }
}