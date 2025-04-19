src.main.java;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class EcommerceWebScraper {
    
    // Product class to store scraped data
    static class Product {
        private String name;
        private String price;
        private String rating;
        private String url;
        
        public Product(String name, String price, String rating, String url) {
            this.name = name;
            this.price = price;
            this.rating = rating;
            this.url = url;
        }
        
        @Override
        public String toString() {
            return "Product [name=" + name + ", price=" + price + ", rating=" + rating + ", url=" + url + "]";
        }
        
        public String toCsvRow() {
            // Escape quotes and special characters for CSV format
            String escapedName = name.replace("\"", "\"\"");
            return "\"" + escapedName + "\"," + price + "," + rating + ",\"" + url + "\"";
        }
    }
    
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    public static void main(String[] args) {
        try {
            // Target URL - we'll use Books section of BookDepository which allows scraping
            String targetUrl = "https://www.bookdepository.com/bestsellers";
            
            // Fetch the HTML content
            String htmlContent = fetchWebpage(targetUrl);
            
            // Parse and extract products
            List<Product> products = extractProducts(htmlContent, targetUrl);
            
            // Save to CSV
            String csvFilePath = "book_products.csv";
            saveToCSV(products, csvFilePath);
            
            System.out.println("Successfully scraped " + products.size() + " products and saved to " + csvFilePath);
            products.forEach(System.out::println);
            
        } catch (Exception e) {
            System.err.println("Error occurred during web scraping: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String fetchWebpage(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .setHeader("User-Agent", "Java Web Scraper / Educational Purpose")
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new IOException("Failed to fetch webpage. Status code: " + response.statusCode());
        }
    }
    
    private static List<Product> extractProducts(String html, String baseUrl) {
        List<Product> products = new ArrayList<>();
        
        Document document = Jsoup.parse(html);
        
        // Select product elements - this is based on BookDepository's structure
        Elements productElements = document.select(".book-item");
        
        for (Element productElement : productElements) {
            try {
                // Extract product details
                Element nameElement = productElement.selectFirst(".title a");
                Element priceElement = productElement.selectFirst(".price");
                Element ratingElement = productElement.selectFirst(".rating-wrap");
                
                if (nameElement != null && priceElement != null) {
                    String name = nameElement.text().trim();
                    String price = priceElement.text().trim();
                    String url = baseUrl;
                    
                    // Extract URL if available
                    if (nameElement.hasAttr("href")) {
                        String relativeUrl = nameElement.attr("href");
                        // Make sure we have absolute URL
                        if (relativeUrl.startsWith("/")) {
                            url = "https://www.bookdepository.com" + relativeUrl;
                        } else {
                            url = relativeUrl;
                        }
                    }
                    
                    // Extract rating if available
                    String rating = "Not rated";
                    if (ratingElement != null) {
                        // Try to extract numeric rating
                        Pattern pattern = Pattern.compile("([0-5](\\.[0-9])?)");
                        Matcher matcher = pattern.matcher(ratingElement.text());
                        if (matcher.find()) {
                            rating = matcher.group(1);
                        }
                    }
                    
                    Product product = new Product(name, price, rating, url);
                    products.add(product);
                }
            } catch (Exception e) {
                System.err.println("Error while extracting product: " + e.getMessage());
            }
        }
        
        return products;
    }
    
    private static void saveToCSV(List<Product> products, String filePath) throws IOException {
        try (FileWriter csvWriter = new FileWriter(filePath)) {
            // Write header
            csvWriter.append("Name,Price,Rating,URL\n");
            
            // Write data rows
            for (Product product : products) {
                csvWriter.append(product.toCsvRow());
                csvWriter.append("\n");
            }
            
            csvWriter.flush();
        }
    }
}