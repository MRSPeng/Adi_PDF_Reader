package com.adisoftwares.bookreader;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.provider.MediaStore.Files;

import com.adisoftwares.bookreader.epub.EpubActivity;
import com.adisoftwares.bookreader.epub.EpubBookData;
import com.adisoftwares.bookreader.pdf.PDFBookData;
import com.adisoftwares.bookreader.pdf.PdfViewActivity;
import com.adisoftwares.bookreader.view.AutofitRecyclerView;

import java.io.File;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by adityathanekar on 03/02/16.
 */
public class BookGridFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String BOOK_LIST = "com.adisoftwares.bookreader.book_list";

    private static final int FILES_LOADER = 0;

    private ArrayList<BookData> booksList;

    private BooksAdapter adapter;

    private BookLoaderTask bookLoaderTask;

    @Bind(R.id.books_recycler_view)
    AutofitRecyclerView recyclerView;
    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(BOOK_LIST, booksList);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.book_grid_fragment, container, false);

        ButterKnife.bind(this, rootView);

        if (savedInstanceState != null)
            booksList = savedInstanceState.getParcelableArrayList(BOOK_LIST);
        else
            booksList = new ArrayList<>();

        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        adapter = new BooksAdapter(getActivity(), booksList);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(new OnItemClickListener() {
                                           @Override
                                           public void onItemClick(View view, int position) {
                                               Intent intent;
                                               if (booksList.get(position).getPath().endsWith(".pdf")) {
                                                   Uri uri = Uri.parse(booksList.get(position).getPath());
                                                   intent = new Intent(getActivity(), PdfViewActivity.class);
                                                   intent.setAction(Intent.ACTION_VIEW);
                                                   intent.setData(uri);

                                                   /*intent = new Intent(getActivity(), PdfViewActivity.class);
                                                   intent.putExtra(PdfViewActivity.EXTRA_FILE_PATH, booksList.get(position).getPath());*/
                                                   startActivity(intent);
                                               } else if (booksList.get(position).getPath().endsWith(".epub")) {
                                                   intent = new Intent(getActivity(), EpubActivity.class);
                                                   intent.putExtra(EpubActivity.EXTRA_FILE_PATH, booksList.get(position).getPath());
                                                   startActivity(intent);
                                               }
                                           }
                                       }

        );

        return rootView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(FILES_LOADER, null, this);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = MediaStore.Files.getContentUri("external");

        String[] projection = null;

        String sortOrder = Files.FileColumns.DATA + " ASC";

        String selection = Files.FileColumns.DATA + " LIKE ?  OR " + Files.FileColumns.DATA + " LIKE ?";
        //String selection = Files.FileColumns.DATA + " LIKE ?";

        String[] selectionArgs = new String[]{"%.pdf", "%.epub"};
        //String[] selectionArgs = new String[]{"%.pdf"};

        CursorLoader cursorLoader = new CursorLoader(getActivity(), uri, projection, selection, selectionArgs, sortOrder);

        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
//        BookData bookData;
//        while (data.moveToNext()) {
//            bookData = null;
//            try {
//                if (data.getString(data.getColumnIndex(Files.FileColumns.DATA)).endsWith(".pdf"))
//                    bookData = new PDFBookData(data.getString(data.getColumnIndex(Files.FileColumns.DATA)), getActivity());
//                else if (data.getString(data.getColumnIndex(Files.FileColumns.DATA)).endsWith(".epub"))
//                    bookData = new EpubBookData(data.getString(data.getColumnIndex(Files.FileColumns.DATA)), getActivity());
//                bookData.setId(data.getLong(data.getColumnIndex(Files.FileColumns._ID)));
//                booksList.add(bookData);
//            } catch (Exception e) {
//                Log.d("Aditya", e.toString());
//            }
//        }
//        adapter.notifyDataSetChanged();
        bookLoaderTask = new BookLoaderTask();
        bookLoaderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(bookLoaderTask!= null)
            bookLoaderTask.cancel(true);
        ButterKnife.unbind(this);
    }

    public class BookLoaderTask extends AsyncTask<Cursor, Integer, Void> {

        @Override
        protected Void doInBackground(Cursor... params) {
            BookData bookData;
            Cursor data = params[0];
            File book;
            while (data.moveToNext()) {
                if(isCancelled()){
                    break;
                }
                bookData = null;
                book = new File(data.getString(data.getColumnIndex(Files.FileColumns.DATA)));
                try {
                    if (book.isDirectory()) {
                        continue;
                    } else {
                        if (book.getAbsolutePath().endsWith(".pdf"))
                            bookData = new PDFBookData(data.getString(data.getColumnIndex(Files.FileColumns.DATA)));
                        else if (book.getAbsolutePath().endsWith(".epub"))
                            bookData = new EpubBookData(data.getString(data.getColumnIndex(Files.FileColumns.DATA)));
                        else
                            continue;
                        bookData.setId(data.getLong(data.getColumnIndex(Files.FileColumns._ID)));
                        booksList.add(bookData);
                        publishProgress(booksList.size() - 1);
                    }
                } catch (Exception e) {
                    Log.d("Aditya", e.toString());
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            adapter.notifyItemInserted(values[0]);
        }
    }
}
