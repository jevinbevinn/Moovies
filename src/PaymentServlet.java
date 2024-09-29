import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "PaymentServlet", urlPatterns = "/api/Payment")
public class PaymentServlet extends HttpServlet implements Parameters {
    private DataSource dataSource;
    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/master");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();

        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String cardNum = request.getParameter("cardNum");
        Date expirationDate;
        // checks if a valid date was given
        try {
            expirationDate = Date.valueOf(request.getParameter("expirationDate"));
        } catch (IllegalArgumentException e) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("message", "re enter payment info");
            jsonObject.addProperty("status", "fail");
            request.getServletContext().log("Payment failed");

            response.getWriter().write(jsonObject.toString());
            out.close();
            return;
        }

        try (out; Connection conn = dataSource.getConnection()) {
            JsonObject responseJsonObject = new JsonObject();

            if (cardNum == null || cardNum.isEmpty() || firstName == null || firstName.isEmpty()
                    || lastName == null || lastName.isEmpty()) {
                responseJsonObject.addProperty("message", "re enter payment info");
                responseJsonObject.addProperty("status", "fail");
                // Log to localhost log
                request.getServletContext().log("Payment failed");

                response.getWriter().write(responseJsonObject.toString());
                out.close();
                return;
            }

            String customerQuery = "SELECT c.id AS customerId " +
                                    "FROM customers c " +
                                    "JOIN creditcards cc ON c.ccId = cc.id " +
                                    "WHERE cc.id = ?" +
                                    "  AND cc.firstName = ?" +
                                    "  AND cc.lastName = ?" +
                                    "  AND cc.expiration = ?;";

            PreparedStatement customerStatement = conn.prepareStatement(customerQuery);
            customerStatement.setString(1, cardNum);
            customerStatement.setString(2, firstName);
            customerStatement.setString(3, lastName);
            customerStatement.setDate(4, expirationDate);
            ResultSet customer = customerStatement.executeQuery();

            if (!customer.next()) {
                responseJsonObject.addProperty("message", "re enter payment info");
                responseJsonObject.addProperty("status", "fail");
                // Log to localhost log
                request.getServletContext().log("Payment failed");
                response.getWriter().write(responseJsonObject.toString());
                return;
            }

            responseJsonObject.addProperty("status", "success");

            HttpSession session = request.getSession();
            Map<String, String> itemIdToSaleIds = (Map<String, String>) session.getAttribute("itemIdToSaleIds");
            if (itemIdToSaleIds == null) {
                itemIdToSaleIds = new HashMap<>();
            }
            List<Map<String, String>> previousItems = (List<Map<String, String>>) session.getAttribute("previousItems");

            String saleIDQuery = "SELECT max(id) as saleID FROM sales;";
            PreparedStatement saleIDStatement = conn.prepareStatement(saleIDQuery);
            ResultSet rs = saleIDStatement.executeQuery();
            int currSaleID = 0;

            if (rs.next()) {
                currSaleID = rs.getInt("saleID") + 1;
            }

            String submitOrderQuery = "INSERT INTO sales (customerId, movieId, saleDate) VALUES (?, ?, ?);";
            PreparedStatement submitOrderStatement = conn.prepareStatement(submitOrderQuery);

            Date currentDate = new Date(System.currentTimeMillis());
            String customerID = customer.getString("customerId");

            for (Map<String, String> item : previousItems) {
                String movieID = item.get("id");

                synchronized (itemIdToSaleIds) {
                    if (itemIdToSaleIds.containsKey(movieID)) {
                        // Concatenate sale IDs if the item already exists
                        String existingSaleIds = itemIdToSaleIds.get(movieID);
                        existingSaleIds = existingSaleIds + ", " + currSaleID;
                        itemIdToSaleIds.put(movieID, existingSaleIds);
                    } else {
                        // If the item doesn't exist, create a new entry
                        itemIdToSaleIds.put(movieID, String.valueOf(currSaleID));
                    }
                }

                submitOrderStatement.setString(1, customerID);
                submitOrderStatement.setString(2, movieID);
                submitOrderStatement.setDate(3, currentDate);

                submitOrderStatement.executeUpdate();
                currSaleID++;
            }

            session.setAttribute("itemIdToSaleIds", itemIdToSaleIds);
            response.getWriter().write(responseJsonObject.toString());
        } catch (SQLException e) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            request.getServletContext().log("Error:", e);
            response.setStatus(500);
        }
    }
}
