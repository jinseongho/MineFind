package me.ingeni.minefind;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

/**
 * Created by jinseongho on 2016. 5. 10..
 */
public class MineMapAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    private Context mContext;
    private int mapSize = 0;

    public MineMapAdapter(Context c, int mapSize) {
        super();
        this.mContext = c;
        this.mapSize = mapSize;
        mInflater = LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return mapSize;
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.row_gird_minemap, null);
        }
        return convertView;
    }
}
