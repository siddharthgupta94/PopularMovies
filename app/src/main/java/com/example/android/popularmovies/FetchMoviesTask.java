package com.example.android.popularmovies;

import android.os.AsyncTask;
import android.support.annotation.StringDef;
import android.util.Log;

import com.example.android.popularmovies.network.Movie;
import com.example.android.popularmovies.network.Movies;
import com.example.android.popularmovies.network.MovieDatabaseService;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by siddharthgupta on 4/25/17.
 */

public class FetchMoviesTask extends AsyncTask<Void, Void, List<Movie>> {

    public final static String MOST_POPULAR = "popular";
    public final static String TOP_RATED = "top_rated";
    public static String LOG_TAG = FetchMoviesTask.class.getSimpleName();
    private final NotifyAboutTaskCompletionCommand mCommand;
    private
    @SORT_BY
    String mSortBy = MOST_POPULAR;

    public FetchMoviesTask(@SORT_BY String sortBy, NotifyAboutTaskCompletionCommand command) {
        mCommand = command;
        mSortBy = sortBy;
    }

    @Override
    protected void onPostExecute(List<Movie> movies) {
        if (movies != null) {
            mCommand.mMovies = movies;
        } else {
            mCommand.mMovies = new ArrayList<>();
        }
        mCommand.execute();
    }

    @Override
    protected List<Movie> doInBackground(Void... params) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://api.themoviedb.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        MovieDatabaseService service = retrofit.create(MovieDatabaseService.class);
        Call<Movies> call = service.discoverMovies(mSortBy,
                BuildConfig.THE_MOVIE_DATABASE_API_KEY);
        try {
            Response<Movies> response = call.execute();
            Movies movies = response.body();
            return movies.getMovies();

        } catch (IOException e) {
            Log.e(LOG_TAG, "A problem occurred talking to the movie db ", e);
        }
        return null;
    }

    @StringDef({MOST_POPULAR, TOP_RATED})
    public @interface SORT_BY {
    }

    interface Listener {
        void onFetchFinished(Command command);
    }

    public static class NotifyAboutTaskCompletionCommand implements Command {
        private FetchMoviesTask.Listener mListener;
        private List<Movie> mMovies;

        public NotifyAboutTaskCompletionCommand(FetchMoviesTask.Listener listener) {
            mListener = listener;
        }

        @Override
        public void execute() {
            mListener.onFetchFinished(this);
        }

        public List<Movie> getMovies() {
            return mMovies;
        }
    }
}
