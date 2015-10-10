package org.peercast.pecaport.widget;

import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Adapterから表示内容を柔軟に変更できるTableLayout
 */
public class TableView extends TableLayout {
    private static final String TAG = "TableLayout";
    private final DataSetObserver mObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            onAdapterDataChanged();
        }
    };

    private Adapter mAdapter;
    private TableRow mHeader;
    private TableRow mFooter;

    public TableView(Context context) {
        super(context);
    }

    public TableView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    protected void onAdapterChanged() {
        final int nCol = mAdapter.getColumnCount();

        removeAllViews();

        mHeader = mAdapter.createHeaderRow(this);
        if (mHeader != null) {
            if (mHeader.getChildCount() != nCol)
                throw new RuntimeException("mismatch header column count");
            addView(mHeader);
        }

        mFooter = mAdapter.createFooterRow(this);
        if (mFooter != null) {
            if (mFooter.getChildCount() != nCol)
                throw new RuntimeException("mismatch footer column count");
            addView(mFooter);
        }

        onAdapterDataChanged();
    }

    @UiThread
    public void setAdapter(@Nullable Adapter adapter) {
        if (mAdapter == adapter)
            return;
        if (mAdapter != null) {
            mAdapter.unregisterObserver(mObserver);
        }
        mAdapter = adapter;
        if (mAdapter == null) {
            removeAllViews();
        } else {
            mAdapter.registerObserver(mObserver);
            onAdapterChanged();
        }
    }


    private Collection<TableRow> getOrCreateRows(final int newRowCount ) {
        List<TableRow> rowViews = new ArrayList<>(newRowCount);

        for (int i = 0, nRow = getChildCount(); i < nRow ; i++){
            TableRow row = (TableRow)getChildAt(i);
            if (row != mHeader && row != mFooter) {
                rowViews.add(row);
            }
        }

        //足りないので作成
        while (rowViews.size() < newRowCount){
            rowViews.add(mAdapter.createRow(this));
        }

        if (rowViews.size() > newRowCount){
            rowViews = rowViews.subList(0, newRowCount);
        }
        //Log.d(TAG, "newRowCount="+newRowCount + " ["+rowViews);

        final int start = mHeader == null ? 0 : 1;
        final int end = mFooter == null ? getChildCount() : getChildCount() - 1;

        //ヘッダー・フッター以外を一旦削除
        removeViews(start, end - start);
        //Log.d(TAG, "start=" + start + ", count=" + (end - start) + ", now=" + getChildCount());
        //Log.d(TAG, "rowViews="+rowViews);

        for (TableRow r : rowViews) {
            addView(r, start);
        }

        return rowViews;
    }

    protected void onAdapterDataChanged() {
        if (mHeader != null)
            mAdapter.bindHeaderRow(mHeader);
        if (mFooter != null)
            mAdapter.bindFooterRow(mFooter);

        final int nRow = mAdapter.getRowCount();
        final int nCol = mAdapter.getColumnCount();

        int r = 0;
        for (TableRow row : getOrCreateRows(nRow)) {
            mAdapter.bindRow(row, r);
            for (int c = 0; c < nCol; c++) {
                mAdapter.bindView(row.getChildAt(c), r, c);
            }
            r++;
        }
    }



    @Nullable
    public Adapter getAdapter() {
        return mAdapter;
    }

    public abstract static class Adapter {
        private final DataSetObservable mObservable = new DataSetObservable();

        /**
         * テーブルの列数
         */
        @IntRange(from = 0)
        abstract public int getColumnCount();

        /**
         * テーブルの行数
         */
        @IntRange(from = 0)
        abstract public int getRowCount();

        /**
         * 指定された位置に関連するデータを返す
         */
        @Nullable
        abstract public Object getItem(int row, int column);

        /**
         * TableRowを作成し、子要素のViewを追加する。
         */
        @NonNull
        abstract protected TableRow createRow(ViewGroup parent);

        /**
         * ヘッダーとなるTableRowを作成し、子要素のViewを追加する。
         */
        @Nullable
        protected TableRow createHeaderRow(ViewGroup parent) {
            return null;
        }

        /**
         * フッターとなるTableRowを作成し、子要素のViewを追加する。
         */
        @Nullable
        protected TableRow createFooterRow(ViewGroup parent) {
            return null;
        }

        /**
         * ヘッダーTableRowのプロパティを設定します。
         */
        protected void bindHeaderRow(TableRow header) {
        }

        /**
         * フッターTableRowのプロパティを設定します。
         */
        protected void bindFooterRow(TableRow header) {
        }

        /**
         * 指定行TableRowのプロパティを設定します。
         */
        protected void bindRow(TableRow r, int row) {
        }

        /**
         * 指定したセルのプロパティを設定します。
         */
        abstract protected void bindView(View view, int row, int column);

        /**
         * 内部データの変更をビューに通知します
         */
        @UiThread
        public void notifyDataSetChanged() {
            mObservable.notifyChanged();
        }

        public void registerObserver(DataSetObserver observer) {
            mObservable.registerObserver(observer);
        }

        public void unregisterObserver(DataSetObserver observer) {
            mObservable.unregisterObserver(observer);
        }

    }

}
