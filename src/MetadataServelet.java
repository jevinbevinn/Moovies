import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
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
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

@WebServlet(name = "MetadataServelet", urlPatterns = "/api/Metadata")
public class MetadataServelet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json"); // Response mime type

        PrintWriter out = response.getWriter();

        // Create a JSON array to store the table and column information
        JsonArray resultArray = new JsonArray();

        // Get a connection from dataSource and let the resource manager close the connection after usage.
        try (Connection conn = dataSource.getConnection()) {
            // Get database metadata
            DatabaseMetaData metaData = conn.getMetaData();

            ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});

            // to avoid duplicates
            Set<String> processedTables = new HashSet<>();

            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");

                if (!processedTables.contains(tableName)) {
                    JsonObject tableObject = new JsonObject();
                    tableObject.addProperty("table", tableName);

                    // Get columns for each table
                    JsonArray columnsArray = new JsonArray();
                    ResultSet columns = metaData.getColumns(null, null, tableName, null);
                    while (columns.next()) {
                        String columnName = columns.getString("COLUMN_NAME");
                        String columnType = columns.getString("TYPE_NAME");

                        JsonObject columnObject = new JsonObject();
                        columnObject.addProperty("name", columnName);
                        columnObject.addProperty("type", columnType);

                        columnsArray.add(columnObject);
                    }

                    tableObject.add("columns", columnsArray);
                    resultArray.add(tableObject);

                    processedTables.add(tableName);
                }
            }

            // Set response status to 200 (OK)
            response.setStatus(200);
        } catch (Exception e) {
            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        } finally {
            out.write(resultArray.toString());
            out.close();
        }
    }
}