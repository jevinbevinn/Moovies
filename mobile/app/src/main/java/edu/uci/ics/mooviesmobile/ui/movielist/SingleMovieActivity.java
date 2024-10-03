package edu.uci.ics.fabflixmobile.ui.movielist;

import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import edu.uci.ics.fabflixmobile.R;
import edu.uci.ics.fabflixmobile.data.NetworkManager;
import edu.uci.ics.fabflixmobile.ui.login.Helpers;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class SingleMovieActivity extends AppCompatActivity {
    TextView titleView;
    TextView ratingView;
    TextView directorView;
    TextView genresView;
    TextView starsView;

    String movieId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_movie);
        titleView = findViewById(R.id.single_title);
        ratingView = findViewById(R.id.single_rating);
        directorView = findViewById(R.id.single_director);
        genresView = findViewById(R.id.single_genres);
        starsView = findViewById(R.id.single_stars);

        Bundle extras = getIntent().getExtras();
        movieId = extras.getString("movieId");

        String parameters = "?id=" + movieId;

        // use the same network queue across our application
        final RequestQueue queue = NetworkManager.sharedManager(this).queue;
        final StringRequest ajaxRequest = new StringRequest(
                Request.Method.GET,
                Helpers.baseURL + "/api/single-movie" + parameters,
                response -> {
                    try {
                        JSONArray jsonArray = new JSONArray(response);
                        JSONObject jsonObj = jsonArray.getJSONObject(0);

                        String titleYear = jsonObj.getString("title") + " (" + jsonObj.getString("year") + ")";
                        titleView.setText(titleYear);

                        ratingView.setText(jsonObj.getString("rating"));

                        String director = "Director: " + jsonObj.getString("director");
                        directorView.setText(director);

                        String genres = "Genres: " + Arrays.toString(jsonObj.getString("genres").split(", "));
                        genresView.setText(genres);

                        String stars = "Stars: " + jsonObj.getString("star_names");
                        starsView.setText(stars);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                },
                error -> {
                    // error
                    Log.d("singlemovie.error", error.toString());
                }) {
        };
        queue.add(ajaxRequest);
    }
}