package com.medmax.potholedetector.views.adapters;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.medmax.potholedetector.R;
import com.medmax.potholedetector.models.Defect;

import java.util.List;

/**
 * Created by Max Medina on 7/12/2017.
 */

public class DefectAdapter extends ArrayAdapter<Defect> {
    private List<Defect> defects;
    private Context context;

    public DefectAdapter(Context context, List<Defect> defects) {
        super(context, 0, defects);
        this.context = context;
        this.defects = defects;
    }

    private int getDefectClass(int id) {
        switch (id) {
            case R.id.radio_pothole:
                return Defect.ClassType.POTHOLE;

            case R.id.radio_speedbump:
                return Defect.ClassType.SPEED_BUMP;
            case R.id.radio_defect:
                return Defect.ClassType.DEFECT;
            default:
                return Defect.ClassType.NOTHING;
        }
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Defect defect = getItem(position);
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_defect, parent, false);
            holder = new ViewHolder();

            holder.tvBame = (TextView) convertView.findViewById(R.id.tv_item_name);
            holder.radiogp = (RadioGroup) convertView.findViewById(R.id.radiogp_defect_class);

            holder.radiogp.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                    int classtype = getDefectClass(checkedId);
                    defect.setClassType(classtype);
                }
            });
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.tvBame.setText(String.format("Defect #%3d", defect.getId()));
        return convertView;
    }

    public List<Defect> getItems(){
        return defects;
    }

    private static class ViewHolder {
        TextView tvBame;
        RadioGroup radiogp;
    }
}
