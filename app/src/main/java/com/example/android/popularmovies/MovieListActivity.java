package com.example.android.popularmovies;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.example.android.popularmovies.data.MovieContract;
import com.example.android.popularmovies.details.MovieDetailActivity;
import com.example.android.popularmovies.details.MovieDetailFragment;
import com.example.android.popularmovies.network.Movie;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MovieListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        FetchMoviesTask.Listener, MovieListAdapter.Callbacks {

    private static final String EXTRA_MOVIES = "EXTRA_MOVIES";
    private static final String EXTRA_SORT_BY = "EXTRA_SORT_BY";
    @Bind(R.id.movie_list)
    RecyclerView mRecyclerView;
    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    private RetainedFragment mRetainedFragment;
    private MovieListAdapter mAdapter;
    private String mSortBy = FetchMoviesTask.MOST_POPULAR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_list);
        ButterKnife.bind(this);

        mToolbar.setTitle(R.string.title_movie_list);
        setSupportActionBar(mToolbar);

        String tag = RetainedFragment.class.getName();
        this.mRetainedFragment = (RetainedFragment) getSupportFragmentManager().findFragmentByTag(tag);
        if (this.mRetainedFragment == null) {
            this.mRetainedFragment = new RetainedFragment();
            getSupportFragmentManager().beginTransaction().add(this.mRetainedFragment, tag).commit();
        }

        mRecyclerView.setLayoutManager(new GridLayoutManager(this, getResources()
                .getInteger(R.integer.grid_number_cols)));
        mAdapter = new MovieListAdapter(new ArrayList<Movie>(), this);
        mRecyclerView.setAdapter(mAdapter);


        if (savedInstanceState != null) {
            mSortBy = savedInstanceState.getString(EXTRA_SORT_BY);
            if (savedInstanceState.containsKey(EXTRA_MOVIES)) {
                List<Movie> movies = savedInstanceState.getParcelableArrayList(EXTRA_MOVIES);
                mAdapter.add(movies);
                findViewById(R.id.progress).setVisibility(View.GONE);
            }
            updateEmptyState();
        } else {
            fetchMovies(mSortBy);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        ArrayList<Movie> movies = mAdapter.getMovies();
        if (movies != null && !movies.isEmpty()) {
            outState.putParcelableArrayList(EXTRA_MOVIES, movies);
        }
        outState.putString(EXTRA_SORT_BY, mSortBy);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.movie_list_activity, menu);

        switch (mSortBy) {
            case FetchMoviesTask.MOST_POPULAR:
                menu.findItem(R.id.sort_by_most_popular).setChecked(true);
                break;
            case FetchMoviesTask.TOP_RATED:
                menu.findItem(R.id.sort_by_top_rated).setChecked(true);
                break;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sort_by_top_rated:
                mSortBy = FetchMoviesTask.TOP_RATED;
                fetchMovies(mSortBy);
                item.setChecked(true);
                break;
            case R.id.sort_by_most_popular:
                mSortBy = FetchMoviesTask.MOST_POPULAR;
                fetchMovies(mSortBy);
                item.setChecked(true);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void open(Movie movie, int position) {
        Intent intent = new Intent(this, MovieDetailActivity.class);
        intent.putExtra(MovieDetailFragment.ARG_MOVIE, movie);
        startActivity(intent);
    }

    @Override
    public void onFetchFinished(Command command) {
        if (command instanceof FetchMoviesTask.NotifyAboutTaskCompletionCommand) {
            mAdapter.add(((FetchMoviesTask.NotifyAboutTaskCompletionCommand) command).getMovies());
            updateEmptyState();
            findViewById(R.id.progress).setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.add(cursor);
        updateEmptyState();
        findViewById(R.id.progress).setVisibility(View.GONE);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        findViewById(R.id.progress).setVisibility(View.VISIBLE);
        return new CursorLoader(this,
                MovieContract.MovieEntry.CONTENT_URI,
                MovieContract.MovieEntry.MOVIE_COLUMNS,
                null,
                null,
                null);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        // Not used
    }

    private void fetchMovies(String sortBy) {
        findViewById(R.id.progress).setVisibility(View.VISIBLE);
        FetchMoviesTask.NotifyAboutTaskCompletionCommand command =
                new FetchMoviesTask.NotifyAboutTaskCompletionCommand(this.mRetainedFragment);
        new FetchMoviesTask(sortBy, command).execute();
    }

    private void updateEmptyState() {
        if (mAdapter.getItemCount() == 0) {
            findViewById(R.id.empty_state_container).setVisibility(View.VISIBLE);
        }
    }

    public static class RetainedFragment extends Fragment implements FetchMoviesTask.Listener {

        private boolean mPaused = false;
        private Command mWaitingCommand = null;

        public RetainedFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        @Override
        public void onPause() {
            super.onPause();
            mPaused = true;
        }

        @Override
        public void onResume() {
            super.onResume();
            mPaused = false;
            if (mWaitingCommand != null) {
                onFetchFinished(mWaitingCommand);
            }
        }

        @Override
        public void onFetchFinished(Command command) {
            if (getActivity() instanceof FetchMoviesTask.Listener && !mPaused) {
                FetchMoviesTask.Listener listener = (FetchMoviesTask.Listener) getActivity();
                listener.onFetchFinished(command);
                mWaitingCommand = null;
            } else {
                mWaitingCommand = command;
            }
        }
    }
}
