import com.google.gson.JsonObject;
import org.jasypt.util.password.StrongPasswordEncryptor;
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

@WebServlet(name = "DashLoginServlet", urlPatterns = "/api/DashLogin")
public class DashLoginServlet extends HttpServlet {
    private DataSource dataSource;
    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    private void attemptLogin(ResultSet user, String username, HttpServletRequest request, String password,
                              JsonObject responseJsonObject) throws SQLException {
        if (!user.next()) {
            responseJsonObject.addProperty("message", "user " + username + " doesn't exist");
            responseJsonObject.addProperty("status", "fail");
            // Log to localhost log
            request.getServletContext().log("Login failed");
            return;
        }

        String encryptedPassword = user.getString("password");
        boolean success = new StrongPasswordEncryptor().checkPassword(password, encryptedPassword);

        if (!success) {
            responseJsonObject.addProperty("message", "incorrect password");
            responseJsonObject.addProperty("status", "fail");
            // Log to localhost log
            request.getServletContext().log("Login failed");
            return;
        }

        request.getSession().setAttribute("employee", new Employee(username));
        responseJsonObject.addProperty("status", "success");
        responseJsonObject.addProperty("message", "success");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        PrintWriter out = response.getWriter();

        String gRecaptchaResponse = request.getParameter("g-recaptcha-response");
        JsonObject responseJsonObject = new JsonObject();

        try {
            RecaptchaVerifyUtils.verify(gRecaptchaResponse);
        } catch (Exception e) {
            responseJsonObject.addProperty("message", "Please complete the captcha");
            responseJsonObject.addProperty("status", "fail");
            // Log to localhost log
            request.getServletContext().log("recaptcha verification error");

            response.getWriter().write(responseJsonObject.toString());
            out.close();
            return;
        }

        try (out; Connection conn = dataSource.getConnection()) {
            // grab customer with matching "username" from db
            String userQuery = "SELECT * FROM employees WHERE email = ?;";
            PreparedStatement userStatement = conn.prepareStatement(userQuery);
            userStatement.setString(1, username);
            ResultSet user = userStatement.executeQuery();

            attemptLogin(user, username, request, password, responseJsonObject);

            user.close();
            userStatement.close();

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
