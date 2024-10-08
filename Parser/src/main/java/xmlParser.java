import java.io.*;
import java.sql.*;
import java.util.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;

public class xmlParser extends DefaultHandler implements Parameters {
    HashMap<String, Movie> myMovies;
    HashMap<String, ArrayList<String>> mySIMs;

    private String tempVal;

    private String director;

    private Movie tempMovie;
    private Star tempStar;
    ArrayList<String> tempSIMStars;

    private int movieDupe = 0;
    private int starDupe = 0;

    private int movieInconsistent = 0;
    private int moviesNotFound = 0;

    private int starsNotFound = 0;

    private boolean isConsistent = true;

    private boolean isDuplicate = false;

    private boolean isFound = true;

    private int availableGenreId;

    private int availableStarId;

    private int availableMovieId;

    private int gimInserts = 0;

    private int simInserts = 0;

    private int starInserts = 0;

    private String castMovieId;

    HashMap<String, String> catToGenreMap = new HashMap<String, String>() {{
        put("susp", "Thriller");
        put("cnr", "Crime");
        put("cmr", "Crime");
        put("cnrb", "Crime");
        put("cnrbb", "Crime");
        put("dram", "Drama");
        put("dramd", "Drama");
        put("west", "Western");
        put("west1", "Western");
        put("myst", "Mystery");
        put("mystp", "Mystery");
        put("s.f.", "Sci-Fi");
        put("scfi", "Sci-Fi");
        put("scif", "Sci-Fi");
        put("sxfi", "Sci-Fi");
        put("advt", "Adventure");
        put("horr", "Horror");
        put("hor", "Horror");
        put("romt", "Romance");
        put("romtx", "Romance");
        put("ront", "Romance");
        put("ram", "Romance");
        put("comd", "Comedy");
        put("cond", "Comedy");
        put("comdx", "Comedy");
        put("romt comd", "Romantic Comedy");
        put("musc", "Musical");
        put("muusc", "Musical");
        put("muscl", "Musical");
        put("scat", "Musical");
        put("stage musical", "Musical");
        put("docu", "Documentary");
        put("porn", "Adult");
        put("porb", "Adult");
        put("kinky", "Adult");
        put("noir", "Noir");
        put("biop", "Biography");
        put("biopx", "Biography");
        put("biopp", "Biography");
        put("biog", "Biography");
        put("biob", "Biography");
        put("bio", "Biography");
        put("tv", "TV Show");
        put("tvs", "TV Series");
        put("tvm", "TV Miniseries");
        put("tvmini", "TV Miniseries");
        put("actn", "Action");
        put("axtn", "Action");
        put("sctn", "Action");
        put("cart", "Cartoon");
        put("crim", "Crime");
        put("faml", "Family");
        put("fant", "Fantasy");
        put("fanth*", "Fantasy");
        put("hist", "History");
        put("camp now", "Camp");
        put("camp", "Camp");
        put("disa", "Disaster");
        put("dist", "Disaster");
        put("epic", "Epic");
        put("surl", "Surreal");
        put("surr", "Surreal");
        put("surreal", "Surreal");
        put("avga", "Avant Garde");
        put("avant garde", "Avant Garde");
        put("ctxx", "Uncategorized");
        put("ctcxx", "Uncategorized");
        put("ctxxx", "Uncategorized");
        put("txx", "Uncategorized");
        put("cartoon", "Cartoon");
        put("drama", "Drama");
        put("dramn", "Drama");
        put("dram>", "Drama");
        put("draam", "Drama");
        put("duco", "Documentary");
        put("ducu", "Documentary");
        put("dicu", "Documentary");
        put("act", "Action");
        put("viol", "Action");
        put("romt.", "Romance");
        put("romtadvt", "Romance");
        put("sports", "Sports");
        put("psyc", "Psychological");
        put("psych", "Psychological");
        put("anti-dram", "Drama");
        put("adctx", "Adventure");
        put("adct", "Adventure");
        put("h", "Uncategorized");
        put("h0", "Uncategorized");
        put("h*", "Uncategorized");
        put("h**", "Uncategorized");
        put("dram.actn", "Drama");
    }};

    HashMap<String, Integer> existingGenres;
    HashMap<String, Integer> newGenres;

    HashMap<String, ArrayList<Star>> existingStars;
    HashMap<String, ArrayList<Star>> newStars;

    public xmlParser() throws ClassNotFoundException, SQLException {
        myMovies = new HashMap<String, Movie>();
        mySIMs = new HashMap<String, ArrayList<String>>();
        tempSIMStars = new ArrayList<String>();
        existingGenres = new HashMap<String, Integer>();
        newGenres = new HashMap<String, Integer>();
        existingStars = new HashMap<String, ArrayList<Star>>();
        newStars = new HashMap<String, ArrayList<Star>>();

        // mySQL driver
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection("jdbc:" + Parameters.dbtype + ":///" + Parameters.dbname + "?autoReconnect=true&useSSL=true",
                Parameters.username, Parameters.password);) {

            // Construct a query with parameter
            String query = "SELECT * FROM genres;";

            Statement statement = conn.createStatement();

            ResultSet rs = statement.executeQuery(query);

            while(rs.next()) {
                existingGenres.put(rs.getString("name"), rs.getInt("id"));
            }
            rs.close();

            ResultSet rs2 = statement.executeQuery("SELECT MAX(id) FROM genres;");
            if (rs2.next()) {
                availableGenreId = rs2.getInt(1) + 1;
            }
            rs2.close();

            ResultSet rs3 = statement.executeQuery("SELECT MAX(CAST(SUBSTRING(id, 3) AS UNSIGNED)) FROM stars;");
            if (rs3.next()) {
                availableStarId = rs3.getInt(1) + 1;
            }
            rs3.close();

            ResultSet rs4 = statement.executeQuery("SELECT MAX(CAST(SUBSTRING(id, 3) AS UNSIGNED)) FROM movies;");
            if (rs4.next()) {
                availableMovieId = rs4.getInt(1) + 1;
            }
            rs4.close();


            ResultSet rs5 = statement.executeQuery("SELECT * FROM stars;");

            while (rs5.next()) {
                Star star = new Star();
                star.setId(rs5.getString("id"));
                star.setName(rs5.getString("name"));
                star.setBirthYear(Integer.toString(rs5.getInt("birthYear")));

                // Check if name exists
                if (existingStars.containsKey(rs5.getString("name"))) {
                    existingStars.get(rs5.getString("name")).add(star);
                } else {
                    ArrayList<Star> starsArray = new ArrayList<Star>(){{add(star);}};
                    existingStars.put(rs5.getString("name"), starsArray);
                }
            }
            rs5.close();


            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void runExample() {
        parseDocument();

        // Create CSV files
        genreToCSV(newGenres);
        movieToCSV(myMovies);
        starToCSV(newStars);
        simToCSV(mySIMs);

        printData(); // write to csv file
    }

    private void parseDocument() {

        //get a factory
        SAXParserFactory spf = SAXParserFactory.newInstance();
        try {

            //get a new instance of parser
            SAXParser sp = spf.newSAXParser();

            //parse the file and register for call backs
            sp.parse("stanford-movies/mains243.xml", this);
            sp.parse("stanford-movies/actors63.xml", this);
            sp.parse("stanford-movies/casts124.xml", this);

        } catch (SAXException se) {
            se.printStackTrace();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
    }



    /**
     * Iterate through the list and print
     * the contents
     */
    private void printData() {
        // Load all data into database
        loadData("movies.csv", "movies");
        System.out.println("No of Inserted Movies: " + myMovies.size());
        loadData("genres.csv", "genres");
        System.out.println("No of Inserted Genres: " + newGenres.size());
        loadData("stars.csv", "stars");
        System.out.println("No of Inserted Stars: " + starInserts);
        updateAvailableInt();
        loadData("genres_in_movies.csv", "genres_in_movies");
        System.out.println("No of Records Inserted into Genres_in_Movies: " + gimInserts);
        loadData("stars_in_movies.csv", "stars_in_movies");
        System.out.println("No of Records Inserted into Stars_in_Movies: " + simInserts);

        System.out.println("No of Movie Inconsistencies: " + movieInconsistent);
        System.out.println("No of Duplicated Movies: " + movieDupe);
        System.out.println("No of Duplicated Stars: " + starDupe);
        System.out.println("No of Missing Movies: " + moviesNotFound);
        System.out.println("No of Missing Stars: " + starsNotFound);
    }

    public void loadData(String csvPath, String tableName) {
        try (Connection conn = DriverManager.getConnection("jdbc:" + Parameters.dbtype + ":///" + Parameters.dbname + "?useSSL=false&allowLoadLocalInfile=true",
                Parameters.username, Parameters.password);) {

            if (tableName.equals("movies") || tableName.equals("stars") || tableName.equals("genres")) {
                String query = "LOAD DATA LOCAL INFILE ? " +
                        "INTO TABLE " + tableName + " " +
                        "FIELDS TERMINATED BY '|' " +
                        "LINES TERMINATED BY '\n' " +
                        "IGNORE 1 ROWS";

                try (PreparedStatement statement = conn.prepareStatement(query)) {
                    statement.setString(1, csvPath);
                    statement.execute();
                } catch (SQLException e) {
//                    e.printStackTrace(); // Handle or log the exception as needed
                }
            } else if ("genres_in_movies".equals(tableName)) {
                try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
                    String line;
                    reader.readLine(); // Skip the header line

                    while ((line = reader.readLine()) != null) {
                        String[] values = line.split("\\|");
                        String insertQuery = "INSERT INTO " + tableName + " VALUES (?, ?)";

                        try (PreparedStatement insertStatement = conn.prepareStatement(insertQuery)) {
                            insertStatement.setInt(1, Integer.parseInt(values[0].trim())); //  genreId is an INT
                            insertStatement.setString(2, values[1].trim()); //  movieId is a VARCHAR

                            insertStatement.executeUpdate();
                        } catch (SQLException e) {
                            e.printStackTrace(); // Handle or log the exception as needed
                        }
                    }
                } catch (IOException e) {
//                    e.printStackTrace(); // Handle or log the exception as needed
                }
            } else if ("stars_in_movies".equals(tableName)) {
                try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
                    String line;
                    reader.readLine(); // Skip the header line

                    while ((line = reader.readLine()) != null) {
                        String[] values = line.split("\\|");
                        String starId = values[0].trim(); // Trim to remove leading/trailing spaces
                        String movieId = values[1].trim(); // Trim to remove leading/trailing spaces

                        // Check if starId exists in the stars table (case-sensitive)
                        if (!starExists(conn, starId)) {
                            simInserts--;
                            continue; // Skip this entry
                        }

                        String insertQuery = "INSERT INTO " + tableName + " VALUES (?, ?)";

                        try (PreparedStatement insertStatement = conn.prepareStatement(insertQuery)) {
                            insertStatement.setString(1, starId); // starId is a VARCHAR
                            insertStatement.setString(2, movieId); // movieId is a VARCHAR

                            insertStatement.executeUpdate();
                        } catch (SQLException e) {
                            e.printStackTrace(); // Handle or log the exception as needed
                        }
                    }
                } catch (IOException e) {
//                    e.printStackTrace();
                }
            } else {
                System.out.println("Unsupported table: " + tableName);
            }

        } catch (Exception e) {
//            e.printStackTrace();
        }
    }

    // Helper method to check if a star with the given ID exists in the stars table
    private boolean starExists(Connection conn, String starId) throws SQLException {
        String query = "SELECT 1 FROM stars WHERE id = ?";
        try (PreparedStatement statement = conn.prepareStatement(query)) {
            statement.setString(1, starId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public void updateAvailableInt () {
        try (Connection conn = DriverManager.getConnection("jdbc:" + Parameters.dbtype + ":///" + Parameters.dbname + "?autoReconnect=true&useSSL=true",
                Parameters.username, Parameters.password);) {

            // Construct a query with parameter
            String query = "SELECT * FROM genres;";

            Statement statement = conn.createStatement();

            ResultSet rs = statement.executeQuery(query);

            while(rs.next()) {
                existingGenres.put(rs.getString("name"), rs.getInt("id"));
            }
            rs.close();

            ResultSet rs2 = statement.executeQuery("SELECT MAX(id) FROM genres;");
            if (rs2.next()) {
                availableGenreId = rs2.getInt(1) + 1;
            }
            rs2.close();

            ResultSet rs3 = statement.executeQuery("SELECT MAX(CAST(SUBSTRING(id, 3) AS UNSIGNED)) FROM stars;");
            if (rs3.next()) {
                availableStarId = rs3.getInt(1) + 1;
            }
            rs3.close();

            ResultSet rs4 = statement.executeQuery("SELECT MAX(CAST(SUBSTRING(id, 3) AS UNSIGNED)) FROM movies;");
            if (rs4.next()) {
                availableMovieId = rs4.getInt(1) + 1;
            }
            rs4.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeToTextFile(String fileName, String content) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));
            writer.write(content);
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void movieToCSV( HashMap<String, Movie> movieMap ) {
        try {
            BufferedWriter moviesWriter = new BufferedWriter(new FileWriter("movies.csv", true));
            BufferedWriter gimWriter = new BufferedWriter(new FileWriter("genres_in_movies.csv", true));

            moviesWriter.write("id");
            moviesWriter.write("|");
            moviesWriter.write("title");
            moviesWriter.write("|");
            moviesWriter.write("year");
            moviesWriter.write("|");
            moviesWriter.write("director");
            moviesWriter.newLine();

            gimWriter.write("genreId");
            gimWriter.write("|");
            gimWriter.write("movieId");
            gimWriter.newLine();

            for (Map.Entry<String, Movie> entry : movieMap.entrySet()) { // for writing into movies

                moviesWriter.write(entry.getValue().getId());
                moviesWriter.write("|");
                moviesWriter.write(entry.getValue().getTitle());
                moviesWriter.write("|");
                moviesWriter.write(entry.getValue().getYear());
                moviesWriter.write("|");
                moviesWriter.write(entry.getValue().getDirector());
                moviesWriter.newLine();

                for (String genre: entry.getValue().getGenres()) { // for writing into genres_in_movies
                    String genreId = "";
                    if ( existingGenres.containsKey(genre) ) {
                        genreId = Integer.toString(existingGenres.get(genre));
                    } else {
                        genreId = Integer.toString(newGenres.get(genre));
                    }
                    gimWriter.write(genreId);
                    gimWriter.write("|");
                    gimWriter.write(entry.getValue().getId());
                    gimWriter.newLine();
                    gimInserts++;
                }
            }
            moviesWriter.flush();
            moviesWriter.close();
            gimWriter.flush();
            gimWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //
    public void genreToCSV( HashMap<String, Integer> genreMap ) {
        try {
            BufferedWriter gWriter = new BufferedWriter(new FileWriter("genres.csv", true));
            gWriter.write("id");
            gWriter.write("|");
            gWriter.write("name");
            gWriter.newLine();
            for (Map.Entry<String, Integer> entry : genreMap.entrySet()) {
                gWriter.write(Integer.toString(entry.getValue()));
                gWriter.write("|");
                gWriter.write(entry.getKey());
                gWriter.newLine();
            }
            gWriter.flush();
            gWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void starToCSV( HashMap<String, ArrayList<Star>> starMap ) {
        try {
            BufferedWriter sWriter = new BufferedWriter(new FileWriter("stars.csv", true));
            sWriter.write("id");
            sWriter.write("|");
            sWriter.write("name");
            sWriter.write("|");
            sWriter.write("birthYear");
            sWriter.newLine();
            for (Map.Entry<String, ArrayList<Star>> entry : starMap.entrySet()) {
                for(Star s : entry.getValue()) {
                    sWriter.write(s.getId());
                    sWriter.write("|");
                    sWriter.write(s.getName());
                    if ( s.getBirthYear() != null ) {
                        sWriter.write("|");
                        sWriter.write(s.getBirthYear());
                    }
                    else{
                        sWriter.write("|");
                        sWriter.write("");
                    }
                    sWriter.newLine();
                    starInserts++;
                }
            }
            sWriter.flush();
            sWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void simToCSV( HashMap<String, ArrayList<String>> simMap ) {
        try {
            BufferedWriter simWriter = new BufferedWriter(new FileWriter("stars_in_movies.csv", true));
            simWriter.write("starId");
            simWriter.write("|");
            simWriter.write("movieId");
            simWriter.newLine();
            for (Map.Entry<String, ArrayList<String>> entry : simMap.entrySet()) {
                for(String starId : entry.getValue()) {
                    simWriter.write(starId);
                    simWriter.write("|");
                    simWriter.write(entry.getKey());
                    simWriter.newLine();
                    simInserts++;
                }
            }
            simWriter.flush();
            simWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }





    //Event Handlers
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        tempVal = "";
        if (qName.equalsIgnoreCase("film")) {
            // Create new instance of Movie
            tempMovie = new Movie();
        } else if (qName.equalsIgnoreCase("actor")) {
            // Create new instance of Star
            tempStar = new Star();
        } else if (qName.equalsIgnoreCase("filmc")) {
            // Create new array for holding starIds
            tempSIMStars = new ArrayList<String>();
        }

    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        tempVal = new String(ch, start, length);
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {

        if ( qName.equalsIgnoreCase("dirname")) {
            // Check for inconsistency ( "Unknown" / "" )
            if ( tempVal.toLowerCase().strip().startsWith("unknown") ) {
                isConsistent = false;
            } else if (tempVal.strip().equals("")) {
                isConsistent = false;
            } else { // Set director variable
                director = tempVal.strip();
            }

        } else if (qName.equalsIgnoreCase("directorfilms")) { // checks for closing element for <directorfilms>
            director = null; //reset director name

        } else if (qName.equalsIgnoreCase("film")) {
            tempMovie.setDirector(director);
            if( !isConsistent ) { // If data is inconsistent
                movieInconsistent++;
                writeToTextFile("InconsistentMovies.txt", tempMovie.toString());
                isConsistent = true;
            } else if(isDuplicate) { // If movie is a duplicate
                movieDupe++;
                writeToTextFile("DuplicateMovies.txt", tempMovie.toString());
                isDuplicate = false;
            }else {
                if(tempMovie.getGenres().size() == 0) { // No genres associated with movie
                    movieInconsistent++;
                    writeToTextFile("InconsistentMovies.txt", tempMovie.toString());
                }
                else if (director == null) { // No director associated with movie
                    movieInconsistent++;
                    writeToTextFile("InconsistentMovies.txt", tempMovie.toString());
                }
                else { // Add to myMovies HashMap
                    String fid = tempMovie.getId();
                    String movieId = "tt" + String.format("%07d", availableMovieId);
                    availableMovieId++;
                    tempMovie.setId(movieId);
                    myMovies.put(fid, tempMovie);
                }
            }

        } else if (qName.equalsIgnoreCase("fid")) {

            if( myMovies.containsKey(tempVal.strip()) ){ // Check for duplicate
                isDuplicate = true;
            }
            tempMovie.setId(tempVal.strip()); // Add id to current tempMovie

        } else if (qName.equalsIgnoreCase("t")) {
            // store for dupe checking
            if(tempVal.strip().equals("")) { // No title associated with movie
                isConsistent = false;
            }
            else if(tempVal == null) { // No title associated with movie
                isConsistent = false;
            }
            else {
                tempMovie.setTitle(tempVal.strip());
            }

        }else if (qName.equalsIgnoreCase("year")) {
            // there are some with invalid ints ex: 199x, 19yy, etc
            // report as inconsistent
            if(tempVal.strip().equals("")) { // No year associated with movie
                isConsistent = false;
            }
            else if(tempVal == null) {
                isConsistent = false;
            }
            else {
                try {
                    Integer.parseInt(tempVal.strip());
                } catch (Exception e) {
                    isConsistent = false;
                }
                tempMovie.setYear(tempVal.strip());
            }
        } else if(qName.equalsIgnoreCase("cat")) {
            if(tempVal.strip().equals("")) { // check if this is correct // in case trailing spaces
                isConsistent = false;
            } else if(tempVal == null) {
                isConsistent = false;
            } else {
                // add to tempMovie
                // have to check genre before adding
                // have to .strip() b/c trailing spaces and .toLowerCase()
                if(catToGenreMap.containsKey(tempVal.strip().toLowerCase())){
                    String genre = catToGenreMap.get(tempVal.strip().toLowerCase());
                    if(existingGenres.containsKey(genre)) {
                        tempMovie.addGenre(genre);
                    }
                    else if(newGenres.containsKey(genre)) {
                        tempMovie.addGenre(genre);
                    }
                    else {
                        // get available int and assign to new genre
                        newGenres.put(genre, availableGenreId);
                        availableGenreId++;
                        tempMovie.addGenre(genre);
                    }
                }
                else {
                    for(String g: tempVal.strip().toLowerCase().split("\\s+")) {
                        if (catToGenreMap.containsKey(g)) {
                            String genre = catToGenreMap.get(g);
                            if (existingGenres.containsKey(genre)) {
                                tempMovie.addGenre(genre);
                            } else if (newGenres.containsKey(genre)) {
                                tempMovie.addGenre(genre);
                            } else {
                                // get available int and assign to new genre
                                newGenres.put(genre, availableGenreId);
                                availableGenreId++;
                                tempMovie.addGenre(genre);
                            }
                        } else {
                            if (existingGenres.containsKey(g)) {
                                tempMovie.addGenre(g);
                            } else if (newGenres.containsKey(g)) {
                                tempMovie.addGenre(g);
                            } else {
                                // get available int and assign to new genre
                                newGenres.put(g, availableGenreId);
                                availableGenreId++;
                                tempMovie.addGenre(g);
                            }
                        }
                    }
                }
            }

        } else if (qName.equalsIgnoreCase("actor")) {
            //add it to the list
            // check if dupe
            // if dupe write to stars dupe file the name and birthyear of star
            // if not add to hashmap
            if(isDuplicate) {
                starDupe++;
                //write to file
                writeToTextFile("DuplicateStars.txt", tempStar.toString());
                isDuplicate = false;
            }
            else {
                // generate an id for star using available int remember to update the id afterwards
                // set id
                // concat with nm and LPAD
                String starId = "nm" + String.format("%07d", availableStarId);
                availableStarId++;
                tempStar.setId(starId);
                if(newStars.containsKey(tempStar.getName())){
                    newStars.get(tempStar.getName()).add(tempStar);
                }
                else {
                    newStars.put(tempStar.getName(), new ArrayList<Star>());
                    newStars.get(tempStar.getName()).add(tempStar);
                }
            }

        } else if (qName.equalsIgnoreCase("stagename")) {
            //duplicate check moved to when dob is obtained
            tempStar.setName(tempVal.strip());
        } else if (qName.equalsIgnoreCase("dob")) {
            // if empty set null
            try {
                Integer.parseInt(tempVal.strip());
                tempStar.setBirthYear(tempVal.strip());
                if(existingStars.containsKey(tempStar.getName())){
                    for( Star s : existingStars.get(tempStar.getName())) {
                        if(tempVal.strip().equals(s.getBirthYear())) {
                            isDuplicate = true;
                            break;
                        }
                    }
                }
            }
            catch(Exception e) {
                // set null
                if(existingStars.containsKey(tempStar.getName())){
                    isDuplicate = true;
                }
                tempStar.setBirthYear(null);
            }
        } else if (qName.equalsIgnoreCase("filmc")) {
            //
            if ( isFound ) {
                mySIMs.put(myMovies.get(castMovieId).getId(),tempSIMStars);
            }
            else {
                writeToTextFile("NonExistentMovies.txt", castMovieId + tempSIMStars.toString());
                moviesNotFound++;
                isFound = true;
            }

        } else if (qName.equalsIgnoreCase("f")) {
            // check if exists
            // if not report as missing
            if ( !myMovies.containsKey(tempVal.strip()) ) {
                isFound = false;
            }
            castMovieId = tempVal.strip();
        }else if (qName.equalsIgnoreCase("a")) {
            // check if exists
            // if not report as missing
            // if exists find id
            // ignore if 's a'
            if (newStars.containsKey(tempVal.strip()) ) {
                String starId = newStars.get(tempVal.strip()).get(0).getId();//get star id
                // add to list
                tempSIMStars.add(starId);
            }
            else if(existingStars.containsKey(tempVal.strip())) {
                String starId = existingStars.get(tempVal.strip()).get(0).getId();//get star id
                // add to list
                tempSIMStars.add(starId);
            }
            else if(!tempVal.strip().equals("s a")){
                starsNotFound++;
                // write to star missing file
                writeToTextFile("NonExistentStars.txt", castMovieId + " " + tempVal.strip());
            }

        }

    }

    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        xmlParser sp = new xmlParser();
        sp.runExample();
    }

}
