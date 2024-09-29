import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.ResultSet;

@WebServlet(name = "InsertMovieServlet", urlPatterns = "/api/InsertMovie")
public class InsertMovieServlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/master");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json"); // Response mime type

        // Retrieve parameter id from url request.
        String title = request.getParameter("title");
        String year = request.getParameter("year");
        String director = request.getParameter("director");
        String star = request.getParameter("star");
        String genre = request.getParameter("genre");

        // The log message can be found in localhost log
        request.getServletContext().log("getting title: " + title);
        request.getServletContext().log("getting year: " + year);
        request.getServletContext().log("getting director: " + director);
        request.getServletContext().log("getting star: " + star);
        request.getServletContext().log("getting genre: " + genre);

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (out; Connection conn = dataSource.getConnection()) {

            // Construct a query with parameter represented by "?"
            String query = "CALL add_movie(?, ?, ?, ? ,?);";

            PreparedStatement statement = conn.prepareStatement(query);

            statement.setString(1, title);
            statement.setInt(2, Integer.parseInt(year));
            statement.setString(3, director);
            statement.setString(4, star);
            statement.setString(5, genre);

            ResultSet rs = statement.executeQuery();

            JsonObject jsonObject = new JsonObject();

            while(rs.next()) {
                jsonObject.addProperty("message", rs.getString("message"));
            }
            rs.close();
            statement.close();

            // Write JSON string to output
            out.write(jsonObject.toString());
            // Set response status to 200 (OK)
            response.setStatus(200);
        } catch (Exception e) {
            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        }
    }
}
