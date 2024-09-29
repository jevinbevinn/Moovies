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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashMap;

/**
 * This IndexServlet is declared in the web annotation below,
 * which is mapped to the URL pattern /api/Cart.
 */
@WebServlet(name = "CarServlet", urlPatterns = "/api/Cart")
public class CartServlet extends HttpServlet {
    /**
     * handles GET requests to store session information
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        JsonArray itemsJsonArray = new JsonArray();

        List<Map<String, String>> previousItems = (List<Map<String, String>>) session.getAttribute("previousItems");
        if (previousItems == null) {
            previousItems = new ArrayList<>();
        }

        for (Map<String, String> item : previousItems) {
            JsonObject itemJsonObject = new JsonObject();
            itemJsonObject.addProperty("id", item.get("id"));
            itemJsonObject.addProperty("name", item.get("name"));
            itemsJsonArray.add(itemJsonObject);
        }

        // Build the response JSON
        JsonObject responseJsonObject = new JsonObject();
        responseJsonObject.add("previousItems", itemsJsonArray);

        // write the response JSON
        response.getWriter().write(responseJsonObject.toString());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String id = request.getParameter("movieId");
        String name = request.getParameter("title");
        String removeItem = request.getParameter("remove");
        String operation = request.getParameter("operation");

        HttpSession session = request.getSession();
        List<Map<String, String>> previousItems = (List<Map<String, String>>) session.getAttribute("previousItems");
        if (previousItems == null) {
            previousItems = new ArrayList<>();
        }

        // If remove is present, remove the item
        if (removeItem != null && !removeItem.isEmpty()) {
            previousItems.removeIf(item -> item.get("id").equals(removeItem));
        } else if ("decrement".equals(operation)) {
            // Remove one instance with the  ID
            Collections.reverse(previousItems); // Reverse the list
            Iterator<Map<String, String>> iterator = previousItems.iterator();
            while (iterator.hasNext()) {
                Map<String, String> item = iterator.next();
                if (item.get("id").equals(id)) {
                    iterator.remove();
                    break; // Remove only one instance
                }
            }
            Collections.reverse(previousItems); // Revert to original order
        } else {
            // Create a dictionary for the new item
            Map<String, String> newItem = new HashMap<>();
            newItem.put("id", id);
            newItem.put("name", name);

            synchronized (previousItems) {
                previousItems.add(newItem);
            }
        }

        // Update the session attribute
        session.setAttribute("previousItems", previousItems);

        // Respond with the updated list of items
        JsonArray itemsJsonArray = new JsonArray();
        for (Map<String, String> item : previousItems) {
            JsonObject itemJsonObject = new JsonObject();
            itemJsonObject.addProperty("id", item.get("id"));
            itemJsonObject.addProperty("name", item.get("name"));
            itemsJsonArray.add(itemJsonObject);
        }

        JsonObject responseJsonObject = new JsonObject();
        responseJsonObject.add("previousItems", itemsJsonArray);

        response.getWriter().write(responseJsonObject.toString());
    }
}