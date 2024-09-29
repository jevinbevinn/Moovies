import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.stream.Collectors;

@WebServlet(name = "SearchServlet", urlPatterns = "/api/Search")
public class SearchServlet extends HttpServlet {
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

    private String getFirstThree(String array) {
        String[] itemArray = array.split(",");
        return String.join(", ", Arrays.copyOf(itemArray, Math.min(itemArray.length, 3)));
    }

    private String processTitle(String query) {
        return Arrays.stream(query.split(" "))
                .map(str -> "+" + str + "* ")
                .collect(Collectors.joining())
                .trim();
    }

    private String createBaseQuery(String title, String year, String director, String star, String genre,
                                 String sortingOption, String limit, String page) {
        String query = "SELECT" +
                "    m.id AS movieId," +
                "    m.title," +
                "    m.year," +
                "    m.director," +
                "    IFNULL(r.rating, 'N/A') AS rating," +
                "    (SELECT GROUP_CONCAT(g.name ORDER BY g.name ASC) " +
                "        FROM genres_in_movies AS gim " +
                "        LEFT JOIN genres AS g ON gim.genreId = g.id " +
                "        WHERE gim.movieId = m.id) AS genres, " +
                "    (SELECT GROUP_CONCAT(s.name ORDER BY s.name ASC) " +
                "        FROM stars_in_movies AS sim " +
                "        LEFT JOIN stars AS s ON sim.starid = s.id " +
                "        WHERE sim.movieId = m.id) AS stars, " +
                "    (SELECT GROUP_CONCAT(s.id ORDER BY s.name ASC) " +
                "        FROM stars_in_movies AS sim " +
                "        LEFT JOIN stars AS s ON sim.starid = s.id " +
                "        WHERE sim.movieId = m.id) AS star_ids" +
                " FROM movies AS m " +
                " LEFT JOIN ratings AS r ON m.id = r.movieId " +
                " WHERE";

        if (title != null) {
            query += " MATCH(m.title) AGAINST(? IN BOOLEAN MODE) AND";
        }

        if (year != null) {
            query += " (m.year = ?) AND";
        }

        if (director != null) {
            query += " (m.director LIKE ?) AND";
        }

        if (star != null) {
            query += " m.id IN (SELECT DISTINCT movieId " +
                    " FROM stars_in_movies AS sim " +
                    " LEFT JOIN stars AS s ON sim.starId = s.id " +
                    " WHERE (s.name LIKE ?)" +
                    ")";
        }

        if (genre != null) {
            query += " m.id IN (SELECT DISTINCT movieId " +
                    " FROM genres_in_movies AS gim " +
                    " LEFT JOIN genres AS g ON gim.genreId = g.id " +
                    " WHERE (g.name LIKE ?)" +
                    ")";
        }

        // Removing unnecessary AND from the end of the query
        query = query.replaceAll("AND\\s*$", "");

        // Default option
        if (sortingOption != null) {
            switch (sortingOption) {
                case "titleRatingAA":
                    query += " ORDER BY m.title ASC, IFNULL(r.rating, 'N/A') ASC";
                    break;
                case "titleRatingAD":
                    query += " ORDER BY m.title ASC, IFNULL(r.rating, 'N/A') DESC";
                    break;
                case "titleRatingDA":
                    query += " ORDER BY m.title DESC, IFNULL(r.rating, 'N/A') ASC";
                    break;
                case "titleRatingDD":
                    query += " ORDER BY m.title DESC, IFNULL(r.rating, 'N/A') DESC";
                    break;
                case "ratingTitleAA":
                    query += " ORDER BY IFNULL(r.rating, 'N/A') ASC, m.title ASC";
                    break;
                case "ratingTitleAD":
                    query += " ORDER BY IFNULL(r.rating, 'N/A') ASC, m.title DESC";
                    break;
                case "ratingTitleDA":
                    query += " ORDER BY IFNULL(r.rating, 'N/A') DESC, m.title ASC";
                    break;
                case "ratingTitleDD":
                    query += " ORDER BY IFNULL(r.rating, 'N/A') DESC, m.title DESC";
                    break;
            }
        }

        if (limit != null) {
            query += " LIMIT ?";
        }

        if (page != null) {
            query += " OFFSET ?";
        }

        return query + ";";
    }

    private void setSessionAttributes(HttpSession session, String title, String year, String director, String star,
                                      String genre, String sortingOption, String limit, String page) {
        session.setAttribute("title", title);
        session.setAttribute("year", year);
        session.setAttribute("director", director);
        session.setAttribute("star", star);
        session.setAttribute("genre", genre);
        session.setAttribute("sortingOption", sortingOption);
        session.setAttribute("limit", limit);
        session.setAttribute("page", page);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        long startTimeTS = System.nanoTime();
        long elapsedTimeTJ = 0;
        
        response.setContentType("application/json"); // Response mime type
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession();

        String title = request.getParameter("title");
        String year = request.getParameter("year");
        String director = request.getParameter("director");
        String star = request.getParameter("star");
        String genre = request.getParameter("genre");
        String sortingOption = request.getParameter("sorting");
        String limit = request.getParameter("limit");
        String page = request.getParameter("page");

        // Get the last values from the session
        if (title == null && year == null && director == null && star == null && genre == null && sortingOption == null && limit == null && page == null) {
            title = (String) session.getAttribute("title");
            year = (String) session.getAttribute("year");
            director = (String) session.getAttribute("director");
            star = (String) session.getAttribute("star");
            genre = (String) session.getAttribute("genre");
            sortingOption = (String) session.getAttribute("sortingOption");
            limit = (String) session.getAttribute("limit");
            page = (String) session.getAttribute("page");
        }


        // Update the last values with the current values
        if  (!(title == null && year == null && director == null && star == null && genre == null)) {
            if ( sortingOption == null && limit == null && page == null) {
                sortingOption = "titleRatingAA";
                limit = "10";
                page = "1";
            }

            // Save the updated values to the session
            setSessionAttributes(session, title, year, director, star, genre, sortingOption, limit, page);
        }

        try (out; Connection conn = dataSource.getConnection()) {
            long startTimeTJ = System.nanoTime();
            // Declare our statement
            String query = createBaseQuery(title, year, director, star, genre, sortingOption, limit, page);

            // Declare out statement
            PreparedStatement statement = conn.prepareStatement(query);

            // get parameters
            int parameterIndex = 1;

            if (genre != null) {
                statement.setString(parameterIndex, "%" + genre + "%");
                parameterIndex++;
            }

            if (title != null) {
                if (title.equals("*")) {
                    statement.setString(parameterIndex, "^[^A-Za-z0-9]");
                } else {
                    statement.setString(parameterIndex, processTitle(title));
                }
                parameterIndex++;
            }

            if (year != null) {
                statement.setString(parameterIndex, year);
                parameterIndex++;
            }

            if (director != null) {
                statement.setString(parameterIndex, "%" + director + "%");
                parameterIndex++;
            }

            if (star != null) {
                statement.setString(parameterIndex, "%" + star + "%");
                parameterIndex++;
            }

            if (limit != null && page != null) {
                statement.setInt(parameterIndex, Integer.parseInt(limit));
                parameterIndex++;

                int startPage = (Integer.parseInt(page) - 1) * Integer.parseInt(limit);
                statement.setInt(parameterIndex, startPage);
            }

            // Perform the query
            ResultSet rs = statement.executeQuery();

            JsonArray jsonArray = new JsonArray();

            // Iterate through each row of rs
            while (rs.next()) {
                String movieId = rs.getString("movieId");
                String movieTitle = rs.getString("title");
                int movieYear = rs.getInt("year");
                String movieDirector = rs.getString("director");
                String genres = rs.getString("genres");
                String starNames = rs.getString("stars");
                String starIds = rs.getString("star_ids");
                String rating = rs.getString("rating");
                String firstThreeStarsIds = "";
                String firstThreeStarNames = "";

                if (starIds != null) {
                    firstThreeStarsIds = getFirstThree(starIds);
                    firstThreeStarNames = getFirstThree(starNames);
                }

                JsonObject movieJSON = makeJSON(movieId, movieTitle, movieYear, movieDirector, genres,
                        firstThreeStarNames, firstThreeStarsIds, rating);

                jsonArray.add(movieJSON);
            }
            rs.close();
            statement.close();

            long endTimeTJ = System.nanoTime();
            elapsedTimeTJ = endTimeTJ - startTimeTJ;

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
        }

        long endTimeTS = System.nanoTime();
        long elapsedTimeTS = endTimeTS - startTimeTS;

        System.out.println("uhh: " + elapsedTimeTS);
        try {
            //local path
//                String path = "/Users/chiu7/Desktop/log.txt";
            //aws path
            String path = "/home/log.txt";
            System.out.println(path);
            File file = new File(path);
            FileWriter myWriter = new FileWriter(file, true);
            myWriter.write( elapsedTimeTS + " " + elapsedTimeTJ + "\n");
            myWriter.close();
            System.out.println("Servlet run time: " + elapsedTimeTS + "\nJDBC run time: " + elapsedTimeTJ);
        } catch (IOException ex) {
            System.out.println("An error occurred while writing to file.");
            System.out.println(ex.toString());
        }

    }
}
