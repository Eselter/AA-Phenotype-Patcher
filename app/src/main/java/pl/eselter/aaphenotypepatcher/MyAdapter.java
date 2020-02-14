package pl.eselter.aaphenotypepatcher;

import android.content.SharedPreferences;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MyAdapter extends RecyclerView.Adapter {

    private ArrayList<AppInfo> mAppInfo;

    private RecyclerView mRecyclerView;

    private class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView mName;
        public TextView mPackageName;
        public CheckBox mCheckboxApp;

        public MyViewHolder(View pItem) {
            super(pItem);
            mName = pItem.findViewById(R.id.app_name);
            mPackageName = pItem.findViewById(R.id.app_package_name);
            mCheckboxApp = pItem.findViewById(R.id.checkbox_app);
        }
    }

    public MyAdapter(ArrayList<AppInfo> pAppsInfo, RecyclerView pRecyclerView){
        mAppInfo = pAppsInfo;
        mRecyclerView = pRecyclerView;
        setHasStableIds(true);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, final int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.app_info_layout, viewGroup, false);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickSaveAppsWhiteList(v, i);
                notifyItemChanged(i);
            }
        });

        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, final int i) {
        final AppInfo appInfo = mAppInfo.get(i);
        ((MyViewHolder) viewHolder).mName.setText(appInfo.getName());
        ((MyViewHolder) viewHolder).mPackageName.setText(appInfo.getPackageName());
        ((MyViewHolder) viewHolder).mCheckboxApp.setChecked(appInfo.getIsChecked());

        ((MyViewHolder) viewHolder).mCheckboxApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("eselter", "Clicked checkboc: " + i);
                onClickSaveAppsWhiteList(v, i);
                notifyItemChanged(i);
            }
        });
    }

    private void onClickSaveAppsWhiteList (View v, int position) {
        SharedPreferences appsListPref = v.getContext().getSharedPreferences("appsListPref", 0);
        SharedPreferences.Editor editor = appsListPref.edit();
        if (mAppInfo.get(position).getIsChecked()) {
            editor.remove(mAppInfo.get(position).getPackageName());
            editor.commit();
            mAppInfo.get(position).setIsChecked(false);
            Toast.makeText(v.getContext(), "Removed: " + mAppInfo.get(position).getPackageName(), Toast.LENGTH_SHORT).show();
        } else {
            editor.putString(mAppInfo.get(position).getPackageName(), mAppInfo.get(position).getName());
            editor.commit();
            mAppInfo.get(position).setIsChecked(true);
            Toast.makeText(v.getContext(), "Added: " + mAppInfo.get(position).getPackageName(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return mAppInfo.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }
}
