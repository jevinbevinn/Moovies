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
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;


@WebServlet(name = "InsertStarServlet", urlPatterns = "/api/InsertStar")
public class InsertStarServlet extends HttpServlet {
    private DataSource dataSource;
    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/master");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    // get new id
    private String makeNewID(Connection conn) throws SQLException {
        String query = "SELECT MAX(CAST(SUBSTRING(id, 3) AS SIGNED)) + 1 AS newID FROM stars;";
        try (PreparedStatement statement = conn.prepareStatement(query);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                int newID = rs.getInt("newID");
                return "nm" + String.format("%07d", newID);
            }
        }
        return null;
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json"); // Response mime type
        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        // Doesn't do null checking for birth year
        String name = request.getParameter("name");
        String year = request.getParameter("year");

        try (out; Connection conn = dataSource.getConnection()) {
            String id = makeNewID(conn);

            if (id != null) {
                String query = "INSERT INTO stars (id, name, birthYear) VALUES (?, ?, ?);";
                try (PreparedStatement statement = conn.prepareStatement(query)) {
                    statement.setString(1, id);
                    statement.setString(2, name);

                    if (year != null && !year.isEmpty()) {
                        statement.setString(3, year);
                    } else {
                        statement.setNull(3, Types.INTEGER);
                    }

                    // Perform the query
                    statement.executeUpdate();

                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("message", "Successfully added a star with ID " + id);
                    out.write(jsonObject.toString());
                }
            } else {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("errorMessage", "Failed to generate a new ID for the star.");
                out.write(jsonObject.toString());
                response.setStatus(500);
            }
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
