import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.stream.Collectors;

@WebServlet(name = "AutocompleteServlet", urlPatterns = "/api/Autocomplete")
public class AutocompleteServlet extends HttpServlet{
    private DataSource dataSource;
    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    private JsonObject makeJSON(String movieId, String title) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("value", title);

        JsonObject additionalDataJsonObject = new JsonObject();
        additionalDataJsonObject.addProperty("movieId", movieId);

        jsonObject.add("data", additionalDataJsonObject);
        return jsonObject;
    }

    private String processTitle(String query) {
        return Arrays.stream(query.split(" "))
                .map(str -> "+" + str + "* ")
                .collect(Collectors.joining())
                .trim();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            // setup the response json arrray
            JsonArray jsonArray = new JsonArray();

            // get the query string from parameter
            String query = request.getParameter("query");

            // return the empty json array if query is null or empty
            if (query == null || query.trim().isEmpty()) {
                response.getWriter().write(jsonArray.toString());
                return;
            }
            Connection conn = dataSource.getConnection();
            String moviesQuery = "SELECT id, title" +
                    " FROM movies" +
                    " WHERE MATCH(title) AGAINST(? IN BOOLEAN MODE)" +
                    "LIMIT 10;";

            PreparedStatement statement = conn.prepareStatement(moviesQuery);

            query = processTitle(query);
            statement.setString(1, query);

            ResultSet rs = statement.executeQuery();

            while(rs.next()) {
                String movieId = rs.getString("id");
                String movieTitle = rs.getString("title");

                jsonArray.add(makeJSON(movieId, movieTitle));
            }

            conn.close();
            statement.close();
            rs.close();
            
            response.getWriter().write(jsonArray.toString());
        } catch (Exception e) {
            System.out.println(e);
            response.sendError(500, e.getMessage());
        }
    }
}
