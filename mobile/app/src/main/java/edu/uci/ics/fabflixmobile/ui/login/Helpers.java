package edu.uci.ics.fabflixmobile.ui.login;

import edu.uci.ics.fabflixmobile.data.model.Movie;
import org.json.JSONException;
import org.json.JSONObject;

public class Helpers {
    /*
    In Android, localhost is the address of the device or the emulator.
    To connect to your machine, you need to use the below IP address
    */
//    private static final String host = "10.0.2.2";
    private static final String host = "52.53.244.73";
    private static final String port = "8443";
    private static final String domain = "2023-fall-cs122b-sus";
    public static final String baseURL = "https://" + host + ":" + port + "/" + domain;
    public static Movie createMovie(JSONObject jsonObj) throws JSONException {
        String title = jsonObj.getString("title");
        String movieId = jsonObj.getString("movieId");
        String year = jsonObj.getString("year");
        String director = jsonObj.getString("director");
        String genres = jsonObj.getString("genres");
        String starNames = jsonObj.getString("star_names");
        String rating = jsonObj.getString("rating");

        return new Movie(title, movieId,  year, director, genres, starNames, rating);
    }
}
