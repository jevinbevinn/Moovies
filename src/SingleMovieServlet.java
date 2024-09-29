import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet(name = "SingleMovieServlet", urlPatterns = "/api/single-movie")
public class SingleMovieServlet extends HttpServlet {
    private static final long serialVersionUID = 2L;
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    private JsonObject makeJSON(String title, int year, String director, String genres,
                                String starNames, String starIds, String rating) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("title", title);
        jsonObject.addProperty("year", year);
        jsonObject.addProperty("director", director);
        jsonObject.addProperty("genres", genres);
        jsonObject.addProperty("star_names", starNames);
        jsonObject.addProperty("star_ids", starIds);
        jsonObject.addProperty("rating", rating);

        return jsonObject;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        String movieId = request.getParameter("id");

        PrintWriter out = response.getWriter();

        try (out; Connection conn = dataSource.getConnection()) {
            String query =  "SELECT " +
                    "m.title, " +
                    "m.year, " +
                    "m.director, " +
                    "IFNULL(r.rating, 'N/A') AS rating, " +
                    "(SELECT GROUP_CONCAT(g.name ORDER BY g.name ASC) " +
                    "FROM genres_in_movies AS gim " +
                    "LEFT JOIN genres AS g ON gim.genreId = g.id " +
                    "WHERE gim.movieId = m.id) AS genres, " +
                    "(SELECT GROUP_CONCAT(s.name ORDER BY s.name ASC) " +
                    "FROM stars_in_movies AS sim " +
                    "LEFT JOIN stars AS s ON sim.starId = s.id " +
                    "WHERE sim.movieId = m.id) AS star_names, " +
                    "(SELECT GROUP_CONCAT(s.id ORDER BY s.name ASC) " +
                    "FROM stars_in_movies AS sim " +
                    "LEFT JOIN stars AS s ON sim.starId = s.id " +
                    "WHERE sim.movieId = m.id) AS star_ids " +
                    "FROM movies AS m " +
                    "LEFT JOIN ratings AS r ON m.id = r.movieId " +
                    "WHERE m.id = ?;";


            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, movieId);
            ResultSet rs = statement.executeQuery();

            JsonArray jsonArray = new JsonArray();

            while (rs.next()) {
                String title = rs.getString("title");
                int year = rs.getInt("year");
                String director = rs.getString("director");
                String genres = rs.getString("genres");
                String starNames = rs.getString("star_names");
                String starIds = rs.getString("star_ids");
                String rating = rs.getString("rating");

                starNames = starNames.replace(",", ", ");
                starIds = starIds.replace(",", ", ");


                jsonArray.add(makeJSON(title, year, director, genres, starNames, starIds, rating));
            }
            rs.close();
            statement.close();

            out.write(jsonArray.toString());
            response.setStatus(200);

        } catch (SQLException e) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            request.getServletContext().log("Error:", e);
            response.setStatus(500);
        }
    }
}
