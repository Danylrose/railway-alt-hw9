package com.example.heroku;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;

@Controller
@SpringBootApplication
public class HerokuApplication {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Autowired
    private DataSource dataSource;

    public static void main(String[] args) throws Exception {
        SpringApplication.run(HerokuApplication.class, args);
    }

    @RequestMapping("/")
    String index() {
        return "index";
    }

    // I modified this for the assignment and it works
    @RequestMapping("/db")
    String db(Map<String, Object> model) {
        try (Connection connection = dataSource.getConnection()) {
            Statement stmt = connection.createStatement();

            // Ensure the table exists
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS table_timestamp_and_random_string (" +
                    "tick timestamp, random_string varchar(30))");

            // Insert a new record with the current timestamp and a random string
            String randomString = getRandomString();
            stmt.executeUpdate("INSERT INTO table_timestamp_and_random_string (tick, random_string) " +
                    "VALUES (now(), '" + randomString + "')");

            System.out.println("Print statement inside the main db method. Daniel Rosales-Rodriguez");

            // Retrieve all records from the table
            ResultSet rs = stmt.executeQuery("SELECT tick, random_string FROM table_timestamp_and_random_string");

            // Prepare the output list
            ArrayList<String> output = new ArrayList<>();
            while (rs.next()) {
                output.add("Read from DB: " + rs.getTimestamp("tick") +
                        ", " + rs.getString("random_string"));
            }

            // Add records to the model
            model.put("records", output);

            return "db";
        } catch (Exception e) {
            model.put("message", e.getMessage());
            return "error";
        }
    }

    // use this for user input string for the EC to create the form on your dbinput.html file
    @RequestMapping("/dbinput")
    String dbInput() {
//    return "<!DOCTYPE html>" +
//            "<html>" +
//            "<head><title>Database Input</title></head>" +
//            "<body>" +
//            "<h1>Enter a String to Add to the Database</h1>" +
//            "<form action=\"/dbinput\" method=\"POST\">" +
//            "    <label for=\"userInput\">String:</label>" +
//            "    <input type=\"text\" id=\"userInput\" name=\"userInput\" required>" +
//            "    <button type=\"submit\">Submit</button>" +
//            "</form>" +
//            "</body>" +
//            "</html>";
        return "dbinput";
    }

    // this is the method that will handle the user input string and insert it into the database
    @RequestMapping(value = "/dbinput", method = RequestMethod.POST)
    String handleDbInput(@RequestParam String userInput, Map<String, Object> model) {
        try (Connection connection = dataSource.getConnection()) {
            // Insert the user's input into the database
            String sql = "INSERT INTO table_timestamp_and_random_string (tick, random_string) VALUES (now(), ?)";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, userInput);
            pstmt.executeUpdate();

            System.out.println("Inserted user input into database: " + userInput);

            // Redirect back to the form after submission
            model.put("message", "String added successfully!");
            return "redirect:/db";
        } catch (Exception e) {
            model.put("message", "Error: " + e.getMessage());
            return "error";
        }
    }

    // this generates the random string we are suppose to implement for the assignment
    private String getRandomString() {
        int length = 30; // Maximum length of the random string (matching varchar(30))
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int randomIndex = (int) (Math.random() * characters.length());
            sb.append(characters.charAt(randomIndex));
        }
        return sb.toString();
    }

    @Bean
    public DataSource dataSource() {
        if (dbUrl == null || dbUrl.isEmpty()) {
            return new HikariDataSource();
        } else {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            return new HikariDataSource(config);
        }
    }
}
