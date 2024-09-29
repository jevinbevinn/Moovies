import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.Iterator;

/**
 * This IndexServlet is declared in the web annotation below,
 * which is mapped to the URL pattern /api/Cart.
 */
@WebServlet(name = "ConfirmationServlet", urlPatterns = "/api/Confirmation")
public class ConfirmationServlet extends HttpServlet {
    private void removeSale(Map<String, String> sales, String movieID) {
        if (sales.get(movieID) != null) {
            sales.remove(movieID);
        }
    }

    private JsonObject makeConfirmation(String saleIDs, String movieID, String title) {
        JsonObject itemJsonObject = new JsonObject();
        itemJsonObject.addProperty("saleID", saleIDs);
        itemJsonObject.addProperty("id", movieID);
        itemJsonObject.addProperty("name", title);

        return itemJsonObject;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        JsonArray itemsJsonArray = new JsonArray();
        List<Map<String, String>> previousItems = (List<Map<String, String>>) session.getAttribute("previousItems");
        Map<String, String> itemIdToSaleIds = (Map<String, String>) session.getAttribute("itemIdToSaleIds");

        Iterator<Map<String, String>> itemIter = previousItems.iterator();
        while (itemIter.hasNext()) {
            Map<String, String> item = itemIter.next();
            String movieID = item.get("id");

            JsonObject confirmation = makeConfirmation(itemIdToSaleIds.get(movieID), movieID, item.get("name"));

            itemsJsonArray.add(confirmation);

            this.removeSale(itemIdToSaleIds, movieID);

            itemIter.remove(); // Remove the item from cart
        }

        // Build the response JSON
        JsonObject responseJsonObject = new JsonObject();
        responseJsonObject.add("previousItems", itemsJsonArray);

        // write the response JSON
        response.getWriter().write(responseJsonObject.toString());
    }
}
