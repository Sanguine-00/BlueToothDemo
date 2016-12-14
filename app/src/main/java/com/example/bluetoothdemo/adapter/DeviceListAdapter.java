package com.example.bluetoothdemo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.bluetoothdemo.R;

import java.util.List;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by 张高强 on 2016/12/13.
 * 邮箱: zhang.gaoqiang@mobcb.com
 */

public class DeviceListAdapter extends BaseAdapter {

    private List<Map<Integer, String>> mList;
    private Context mContext;

    public DeviceListAdapter(Context context, List<Map<Integer, String>> list) {
        mContext = context;
        mList = list;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int i) {
        return mList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup group) {
        ViewHolder viewHolder;
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.list_item, null);
            viewHolder = new ViewHolder(view);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        try {
            if (mList.get(i).get(0) != null) {
                viewHolder.mListItemName.setText(mList.get(i).get(0).toString());
            }
            if (mList.get(i).get(1) != null) {
                viewHolder.mListItemAddr.setText(mList.get(i).get(1).toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return view;

    }


    static class ViewHolder {
        @InjectView(R.id.list_item_name)
        TextView mListItemName;
        @InjectView(R.id.list_item_addr)
        TextView mListItemAddr;

        ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}
