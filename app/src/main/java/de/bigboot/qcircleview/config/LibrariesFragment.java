package de.bigboot.qcircleview.config;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import de.bigboot.qcircleview.R;

@EFragment(R.layout.fragment_libraries)
public class LibrariesFragment extends Fragment {
    @ViewById(R.id.libraryList)
    protected RecyclerView libraryList;

    public LibrariesFragment() {
    }

    @AfterViews
    protected void init() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        libraryList.setLayoutManager(layoutManager);
        Adapter adapter = new Adapter(getDataSet());
        libraryList.setAdapter(adapter);
    }

    private ArrayList<Library> getDataSet() {
        ArrayList<Library> results = new ArrayList<Library>();
        results.add(getLibrary("androidslidinguppanel"));
        results.add(getLibrary("androidannotations"));
        results.add(getLibrary("AndroidViewPagerIndicator"));
        results.add(getLibrary("FloatingActionButton"));
        results.add(getLibrary("materialicons"));
        results.add(getLibrary("rootshell"));
        results.add(getLibrary("androidcrop"));
        results.add(getLibrary("roundedimageview"));
        results.add(getLibrary("androidiconify"));
        results.add(getLibrary("listviewanimation"));
        results.add(getLibrary("ion"));

        Collections.sort(results, new Comparator<Library>() {
            @Override
            public int compare(Library lhs, Library rhs) {
                return lhs.name.compareTo(rhs.name);
            }
        });
        return results;
    }

    private Library getLibrary(String name) {
        int authorId = this.getResources().getIdentifier("library_" + name + "_author", "string", this.getActivity().getPackageName());
        int nameId = this.getResources().getIdentifier("library_" + name + "_name", "string", this.getActivity().getPackageName());
        int descId = this.getResources().getIdentifier("library_" + name + "_desc", "string", this.getActivity().getPackageName());
        int urlId = this.getResources().getIdentifier("library_" + name + "_url", "string", this.getActivity().getPackageName());

        Library library = new Library();
        library.author = getResources().getString(authorId);
        library.name = getResources().getString(nameId);
        library.description = getResources().getString(descId);
        library.url = getResources().getString(urlId);

        return library;
    }

    private class Library {
        public String name;
        public String author;
        public String description;
        public String url;

        public Library(String name, String author, String description, String url) {
            this.name = name;
            this.author = author;
            this.description = description;
            this.url = url;
        }

        public Library() {
            this("", "", "", "");
        }
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.LibraryHolder> {
        private ArrayList<Library> mDataset;

        class LibraryHolder extends RecyclerView.ViewHolder {
            TextView name;
            TextView author;
            TextView description;

            public LibraryHolder(View itemView) {
                super(itemView);
                name = (TextView) itemView.findViewById(R.id.name);
                author = (TextView) itemView.findViewById(R.id.author);
                description = (TextView) itemView.findViewById(R.id.description);
            }
        }

        public Adapter(ArrayList<Library> myDataset) {
            mDataset = myDataset;
        }

        @Override
        public LibraryHolder onCreateViewHolder(ViewGroup parent,
                                                int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.card_library, parent, false);

            LibraryHolder dataObjectHolder = new LibraryHolder(view);
            return dataObjectHolder;
        }

        @Override
        public void onBindViewHolder(LibraryHolder holder, int position) {
            holder.name.setText(mDataset.get(position).name);
            holder.author.setText(mDataset.get(position).author);
            holder.description.setText(mDataset.get(position).description);
            final String url = mDataset.get(position).url;
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                }
            });
        }

        public void addItem(Library dataObj, int index) {
            mDataset.add(index, dataObj);
            notifyItemInserted(index);
        }

        public void deleteItem(int index) {
            mDataset.remove(index);
            notifyItemRemoved(index);
        }

        @Override
        public int getItemCount() {
            return mDataset.size();
        }
    }
}
