package com.example.april.hypercastandroid;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * An activity representing a list of OL_Sockets. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link OLSocketDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class OLSocketListActivity extends AppCompatActivity {

    /**
     * Tag used for OLSocketListActivity
     */
    private static final String TAG = "OLSocketListActivity";

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    /**
     * Boolean indicating whether the underlay BT_Module has been started
     */
    private static Boolean  Module_Started = false;

    /**
     * View adapter for recycler view. Act as an interface between raw
     * data in database and the layout
     */
    private SimpleItemRecyclerViewAdapter mAdapter;

    /**
     * Layout manager for recycler view. Linear layout manager is
     * chosen in this case
     */
    private RecyclerView.LayoutManager mLayoutManager;

    /**
     * The default mode of a newly created socket is set to be
     * TCP/UDP Socket. This is also the content displayed in list activity
     */
    private static final String DefaultMode = "TCP/UDP Socket";

    /**
     * An array list that acts as the database. It stores information
     * regarding all sockets that have been created
     */
    public static final List<OLSocketItem> ITEMS = new ArrayList<>();

    /**
     * Tool bar for displaying app name
     */
    private Toolbar toolbar;

    /**
     * Recycler view used to list all overlay sockets created
     */
    private View recyclerView;

    /**
     * Floating Action Button for creating new sockets
     */
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_olsocket_list);

        // Setup for toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        if (findViewById(R.id.olsocket_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        // Setup for recycler view which displays as a list
        recyclerView = findViewById(R.id.olsocket_list);
        assert recyclerView != null;
        mAdapter = new SimpleItemRecyclerViewAdapter(this, ITEMS, mTwoPane);
        mLayoutManager = new LinearLayoutManager(this);

        setupRecyclerView((RecyclerView) recyclerView, mAdapter);
        ((RecyclerView) recyclerView).setLayoutManager(mLayoutManager);

        // Setup for the floating action button
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int current_position = ITEMS.size();

                // Initialize a new item and store it to the datebase
                OLSocketItem newdata = new OLSocketItem(Integer.toString(ITEMS.size()), DefaultMode,"Status: Not Started");
                Intent startIntent = new Intent(getApplicationContext(), OLSocketDetailActivity.class);
                startIntent.putExtra(OLSocketDetailFragment.ARG_ITEM_ID, Integer.toString(ITEMS.size()));

                mAdapter.addItem(newdata,current_position);
                startActivity(startIntent);
            }
        });
    }

    @Override
    protected void onResume() {
        Log.d(TAG,"onResume() called.");
        super.onResume();
        mAdapter.notifyDataSetChanged();
    }


    private void setupRecyclerView(@NonNull RecyclerView recyclerView, SimpleItemRecyclerViewAdapter mAdapter) {
        recyclerView.setAdapter(mAdapter);
    }

    /**
     * Class for setting up recycler view adapter.
     */
    public static class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        /**
         * Activity instance
         */
        private final OLSocketListActivity mParentActivity;

        /**
         * Database used to fill the recycler view
         */
        private final List<OLSocketItem> mValues;

        /**
         * Whether or not the activity is in two-pane mode, i.e. running on a tablet
         * device.
         */
        private final boolean mTwoPane;

        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OLSocketItem item = (OLSocketItem) view.getTag();
                if (mTwoPane) {
                    Bundle arguments = new Bundle();
                    arguments.putString(OLSocketDetailFragment.ARG_ITEM_ID, item.id);
                    OLSocketDetailFragment fragment = new OLSocketDetailFragment();
                    fragment.setArguments(arguments);
                    mParentActivity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.olsocket_detail_container, fragment)
                            .commit();
                } else {
                    Context context = view.getContext();
                    Intent intent = new Intent(context, OLSocketDetailActivity.class);
                    intent.putExtra(OLSocketDetailFragment.ARG_ITEM_ID, item.id);

                    context.startActivity(intent);
                }
            }
        };

        /**
         * Constructor
          * @param parent
         * @param items
         * @param twoPane
         * @return
         */
        SimpleItemRecyclerViewAdapter(OLSocketListActivity parent,
                                      List<OLSocketItem> items,
                                      boolean twoPane) {
            mValues = items;
            mParentActivity = parent;
            mTwoPane = twoPane;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.olsocket_list_content, parent, false);
            return new ViewHolder(view);
        }

        /**
         * Fill each item in the recycler view with info from database
         * @param holder
         * @param position
         */
        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            int id = Integer.parseInt(mValues.get(position).id) + 1;
            holder.mIdView.setText(Integer.toString(id));
            holder.mContentView.setText(mValues.get(position).content);
            holder.mDetailView.setText(mValues.get(position).details);

            holder.itemView.setTag(mValues.get(position));
            holder.itemView.setOnClickListener(mOnClickListener);
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public void addItem (OLSocketItem newItem, int position){
            ITEMS.add(newItem);
            notifyItemInserted(position);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView mIdView;
            final TextView mContentView;
            final TextView mDetailView;

            ViewHolder(View view) {
                super(view);
                mIdView = (TextView) view.findViewById(R.id.id_text);
                mContentView = (TextView) view.findViewById(R.id.content);
                mDetailView = (TextView) view.findViewById(R.id.detail);
            }
        }
    }

    /**
     * A Overlay Socket item representing a piece of content.
     */
    public static class OLSocketItem {

        /**
         * Position ID of the socket in the list
         */
        public String id;

        /**
         * Socket type. Either "TCP/UDP Socket" or "Bluetooth Socket"
         * This is also the name displayed for each socket in the
         * list activity
         */
        public String content;

        /**
         * Current status of the socket. Choose among "Status: Not Started"
         * "Status: Started" or "Status: Stopped"
         */
        public String details;

        /**
         * All the messages that should been printed to the console window
         * when the socket is selected
         */
        public String msg;

        /**
         * Physical address of the socket in TCP/UDP mode
         */
        public String physicaladdr;

        /**
         * A boolean indicating whether the socket is running out not
         */
        public boolean isStarted;

        /**
         * Constructor
         * @param id
         * @param content
         * @param details
         */
        public OLSocketItem(String id, String content, String details) {
            this.id = id;
            this.content = content;
            this.details = details;
            this.msg = "";
            this.physicaladdr = "";
            isStarted = false;
        }

        @Override
        public String toString() {
            return content;
        }
    }

    public static void setModule_Started(Boolean newstate){
        Module_Started = newstate;
    }

    public static Boolean getModule_Started() {
        return Module_Started;
    }

}

