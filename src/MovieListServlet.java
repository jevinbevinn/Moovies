import com.google.gson.JsonArray;
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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;

@WebServlet(name = "MovieListServlet", urlPatterns = "/api/MovieList")
public class MovieListServlet extends HttpServlet implements Parameters {
    private DataSource dataSource;
    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    private JsonObject makeJSON(String movieId, String title, int year, String director, String genres,
                                String firstThreeStarNames, String firstThreeStarIds, String rating) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("movieId", movieId);
        jsonObject.addProperty("title", title);
        jsonObject.addProperty("year", year);
        jsonObject.addProperty("director", director);
        jsonObject.addProperty("genres", genres);
        jsonObject.addProperty("star_names", firstThreeStarNames);
        jsonObject.addProperty("star_ids", firstThreeStarIds);
        jsonObject.addProperty("rating", rating);

        return jsonObject;
    }

    String getFirstThree(String array) {
        String[] itemArray = array.split(",");
        return String.join(", ", Arrays.copyOf(itemArray, Math.min(itemArray.length, 3)));
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json"); // Response mime type
        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (out; Connection conn = dataSource.getConnection()) {
            // Declare our statement
            Statement statement = conn.createStatement();

            String query = "SELECT " +
                    "m.id AS movieId, " +
                    "m.title, " +
                    "m.year, " +
                    "m.director, " +
                    "r.rating, " +
                    "(SELECT GROUP_CONCAT(g.name ORDER BY g.name ASC) " +
                    " FROM genres_in_movies AS gim " +
                    " LEFT JOIN genres AS g ON gim.genreId = g.id " +
                    " WHERE gim.movieId = m.id) AS genres, " +
                    "(SELECT GROUP_CONCAT(s.name ORDER BY s.name ASC) " +
                    " FROM stars_in_movies AS sim " +
                    " LEFT JOIN stars AS s ON sim.starid = s.id " +
                    " WHERE sim.movieId = m.id) AS stars, " +
                    "(SELECT GROUP_CONCAT(s.id ORDER BY s.name ASC) " +
                    " FROM stars_in_movies AS sim " +
                    " LEFT JOIN stars AS s ON sim.starid = s.id " +
                    " WHERE sim.movieId = m.id) AS star_ids " +
                    "FROM movies AS m " +
                    "INNER JOIN ratings AS r ON m.id = r.movieId " +
                    "ORDER BY r.rating DESC " +
                    "LIMIT 20;";

            // Perform the query
            ResultSet rs = statement.executeQuery(query);

            JsonArray jsonArray = new JsonArray();

            // Iterate through each row of rs
            while (rs.next()) {
                String movieId = rs.getString("movieId");
                String title = rs.getString("title");
                int year = rs.getInt("year");
                String director = rs.getString("director");
                String genres = rs.getString("genres");
                String starNames = rs.getString("stars");
                String starIds = rs.getString("star_ids");
                String rating = rs.getString("rating");

                String firstThreeStarIds = getFirstThree(starIds);
                String firstThreeStarNames = getFirstThree(starNames);

                JsonObject movieJSON = makeJSON(movieId, title, year, director, genres,
                        firstThreeStarNames, firstThreeStarIds, rating);
                jsonArray.add(movieJSON);
            }
            rs.close();
            statement.close();

            // Log to localhost log
            request.getServletContext().log("getting " + jsonArray.size() + " results");

            // Write JSON string to output
            out.write(jsonArray.toString());
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
        // Always remember to close db connection after usage. Here it's done by try-with-resources
    }
}
