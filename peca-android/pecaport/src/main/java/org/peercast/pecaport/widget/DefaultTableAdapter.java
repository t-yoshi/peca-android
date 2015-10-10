package org.peercast.pecaport.widget;

import android.support.annotation.IdRes;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class DefaultTableAdapter<T> extends TableView.Adapter {
    @IntRange(from = 0)
    private final int mColumn;
    @IdRes
    private final int mTextViewRes;
    private List<List<T>> mData = new ArrayList<>();

    public DefaultTableAdapter(int numColumn, @IdRes int textViewRes) {
        if (numColumn <= 0)
            throw new IllegalArgumentException("numColumn <= 0");
        mColumn = numColumn;
        mTextViewRes = textViewRes;
    }

    public DefaultTableAdapter(int numColumn) {
        this(numColumn, View.NO_ID);
    }

    final public int getColumnCount() {
        return mColumn;
    }

    public int getRowCount() {
        return mData.size();
    }

    @NonNull
    @Override
    protected TableRow createRow(ViewGroup parent) {
        TableRow row = new TableRow(parent.getContext());
        for (int col = 0; col < getColumnCount(); col++) {
            row.addView(createCell(row, col));
        }
        return row;
    }

    protected View createCell(ViewGroup parent, int column) {
        return new TextView(parent.getContext());
    }

    public void bindRow(TableRow r, int row) {
    }

    public boolean isEnabled(int row, int column) {
        return true;
    }

    public T getItem(int row, int column) {
        if (column >= mColumn)
            throw new IndexOutOfBoundsException("column is " + column + ", but getColumnCount() is " + mColumn);
        return mData.get(row)
                .get(column);
    }

    public void addRow(List<T> values) {
        if (mColumn != values.size())
            throw new IllegalArgumentException("getColumnCount() != values.size()");
        mData.add(values);
    }

    public void addRow(T... values) {
        mData.add(Arrays.asList(values));
    }

    public void remove(int position) {
        mData.remove(position);
    }

    public void addRows(Iterable<List<T>> rows) {
        for (List<T> values : rows)
            addRow(values);
    }

    public void remove(int firstPos, int lastPos) {
        if (firstPos > lastPos)
            throw new IllegalArgumentException("firstPos > lastPos");

        for (int n = lastPos - firstPos; n > 0; n--)
            mData.remove(firstPos);
    }


    /**
     * すべての行データを削除します
     */
    public void removeAll() {
        mData.clear();
    }

    /**
     * getItemで取得したデータをCharSequenceに変換し、TextViewに表示します。
     */
    @Override
    protected void bindView(View view, int row, int column) {
        T value = getItem(row, column);
        boolean enabled = isEnabled(row, column);
        view.setEnabled(enabled);

        CharSequence cs;
        if (value instanceof CharSequence)
            cs = (CharSequence) value;
        else
            cs = value + "";

        TextView textView;
        if (mTextViewRes != View.NO_ID)
            textView = (TextView) view.findViewById(mTextViewRes);
        else
            textView = (TextView) view;

        textView.setText(cs);
    }


    protected static void setColumnsVisibility(TableRow row, int visibility, int... columns){
        for (int c : columns){
            row.getChildAt(c).setVisibility(visibility);
        }
    }

}
