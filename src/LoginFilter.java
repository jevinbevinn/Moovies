import jakarta.servlet.Filter;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Servlet Filter implementation class LoginFilter
 */
@WebFilter(filterName = "LoginFilter", urlPatterns = "/*")
public class LoginFilter implements Filter {
    private final ArrayList<String> allowedURIs = new ArrayList<>();

    /**
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestURI = httpRequest.getRequestURI();

        // Check if this URL is allowed to access without logging in
        if (this.isUrlAllowedWithoutLogin(requestURI)) {
            // Keep default action: pass along the filter chain
            chain.doFilter(request, response);
            return;
        }

        // Check if it's an AJAX request
        boolean isAjax = "XMLHttpRequest".equals(httpRequest.getHeader("X-Requested-With"));

        // Check if a regular user is authenticated
        if (httpRequest.getSession().getAttribute("user") == null) {
            // Check if an employee is authenticated
            if (httpRequest.getSession().getAttribute("employee") == null) {
                if (isAjax) {
                    chain.doFilter(request, response);
                } else {
                    // Redirect to login page for non-AJAX requests
                    httpResponse.sendRedirect("login.html");
                }
            } else {
                // Handle employee-specific logic or allow access
                chain.doFilter(request, response);
            }
        } else {
            // Handle regular user-specific logic or allow access
            chain.doFilter(request, response);
        }
    }

    private boolean isUrlAllowedWithoutLogin(String requestURI) {
        /*
         Setup your own rules here to allow accessing some resources without logging in
         Always allow your own login related requests(html, js, servlet, etc..)
         You might also want to allow some CSS files, etc..
         */
        return allowedURIs.stream().anyMatch(requestURI.toLowerCase()::endsWith);
    }

    public void init(FilterConfig fConfig) {
        allowedURIs.add("login.html");
        allowedURIs.add("login.js");
        allowedURIs.add("api/login");

        allowedURIs.add("css/global.css");

        allowedURIs.add("_dashboard.html");
        allowedURIs.add("js/dashLogin.js");
        allowedURIs.add("api/DashLogin");
    }

    public void destroy() {
        // ignored.
    }
}
