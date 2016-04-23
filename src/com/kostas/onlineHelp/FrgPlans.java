package com.kostas.onlineHelp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.kostas.dbObjects.Plan;
import com.kostas.model.ContentDescriptor;
import com.kostas.model.Database;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liakos on 10/10/2015.
 */
public class FrgPlans extends LoadingOnExitFragment {

    ListView plansListView;
    List<Plan> plans = new ArrayList<Plan>();
    PlansAdapterItem adapterPlans;
    ViewFlipper plansFlipper;
    Button buttonNewPlan, buttonSavePlan;
    EditText planDescription;

    /**
     * Pickers for selecting distance, time between planIntervals, rest start and rounds of planInterval
     */
    NumberPickerKostas planIntervalTimePicker, planIntervalDistancePicker, planIntervalRoundsPicker, planIntervalStartRestPicker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.frg_plans, container, false);
        setViews(v);
        return  v;
    }

    private void setViews(View v){
        plansListView = (ListView) v.findViewById(R.id.plansList);
        new PerformAsyncTask(getActivity()).execute();
        plansFlipper = (ViewFlipper) v.findViewById(R.id.plansFlipper);
        adapterPlans = new PlansAdapterItem(getActivity().getApplicationContext(), R.layout.list_plan_row, plans);
        plansListView.setAdapter(adapterPlans);
        planIntervalTimePicker = (NumberPickerKostas) v.findViewById(R.id.planIntervalTimePicker);
        planIntervalTimePicker.setValue(10);
        planIntervalDistancePicker = (NumberPickerKostas) v.findViewById(R.id.planIntervalDistancePicker);
        planIntervalDistancePicker.setValue(50);
        planIntervalRoundsPicker = (NumberPickerKostas) v.findViewById(R.id.planIntervalRoundsPicker);
        planIntervalStartRestPicker = (NumberPickerKostas) v.findViewById(R.id.planIntervalStartRestPicker);
        planIntervalStartRestPicker.setValue(10);
        planDescription = (EditText) v.findViewById(R.id.planNewDescription);
        buttonNewPlan = ((Button) v.findViewById(R.id.buttonNewPlan));
        buttonSavePlan = ((Button) v.findViewById(R.id.buttonSavePlan));

        buttonNewPlan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                plansFlipper.setDisplayedChild(2);
            }
        });

        buttonSavePlan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                savePlan();
            }
        });
    }

    private void clearViews(){
        planDescription.setText("");
        planIntervalDistancePicker.setValue(100);
        planIntervalRoundsPicker.setValue(0);
        planIntervalStartRestPicker.setValue(10);
        planIntervalTimePicker.setValue(10);
    }

    /**
     * Fetches the plans from the db
     *
     * @param activity
     * @param fromAsync
     */
    void getPlansFromDb(Activity activity, boolean fromAsync){
        Database db = new Database(activity);
        plans.clear();
        List<Plan> newPlans = db.fetchPlansFromDb();
        for (Plan plan : newPlans){
            plans.add(plan);
        }
        if (adapterPlans!=null && !fromAsync) adapterPlans.notifyDataSetChanged();
    }

    /**
     * Toggle list / empty_list_textview visibility
     */
    private void showTextNoPlans(){
        if (plans.size()==0){
            plansFlipper.setDisplayedChild(3);
        }else{
            plansFlipper.setDisplayedChild(1);
        }
    }

    static FrgPlans init(int val) {
        FrgPlans truitonList = new FrgPlans();

        // Supply val input as an argument.
        Bundle args = new Bundle();
        args.putInt("val", val);
        truitonList.setArguments(args);
        return truitonList;
    }



    public class PlansAdapterItem extends ArrayAdapter<Plan> {

        Context mContext;
        int layoutResourceId;
        List<Plan> data;

        public PlansAdapterItem(Context mContext, int layoutResourceId,
                                List<Plan> data) {

            super(mContext, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.mContext = mContext;
            this.data = data;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();

            showTextNoPlans();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            planViewHolder holder =null;
            if (convertView == null || !(convertView.getTag() instanceof planViewHolder)) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.list_plan_row, parent, false);

                holder = new planViewHolder();


                holder.description = (TextView) convertView
                        .findViewById(R.id.planDescription);
                holder.info =  (TextView) convertView
                        .findViewById(R.id.planInfo);

                convertView.setTag(holder);
            } else {
                holder = (planViewHolder) convertView.getTag();

            }

            final Plan plan = plans.get(position);
            holder.description.setText(plan.getDescription());
            holder.info.setText( plan.getMeters()+"m with "+plan.getSeconds()+"secs rest"+ (plan.getRounds()>0 ? " x"+plan.getRounds()+" rounds" : "" ));


            if (position%2==0)
                convertView.setBackgroundDrawable(getResources().getDrawable(R.drawable.plan_even_row));
            else
                convertView.setBackgroundDrawable(getResources().getDrawable(R.drawable.plan_odd_row));


            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    confirmDelete(plan.getId(), position);
                    return false;
                }
            });

            return convertView;

        }

    }

    private class planViewHolder{
        TextView description;
        TextView info;
    }


    private void confirmDelete(final Long trId, final int position){

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                getActivity());
        alertDialogBuilder.setTitle("Confirm")
                .setMessage("Delete Plan ?")
                .setCancelable(false)
                .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        deletePlan(trId, position);
                    }

                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();


    }

    private void savePlan() {


        String description = planDescription.getText().toString().trim();

        if (description.length() < 4 || description.length() > 30){
            Toast.makeText(getActivity(), "Please provide plan name (4-30 chars)", Toast.LENGTH_SHORT).show();
            return;
        }


        Plan newPlan = new Plan();
        newPlan.setId(-1);
        newPlan.setMeters(planIntervalDistancePicker.getValue());
        newPlan.setSeconds(planIntervalTimePicker.getValue());
        newPlan.setRounds(planIntervalRoundsPicker.getValue());
        newPlan.setStartRest(planIntervalStartRestPicker.getValue());
        newPlan.setDescription(description);

        Database db = new Database(getActivity());
        db.addPlan(newPlan);
        getPlansFromDb(getActivity(), false);
        Toast.makeText(getActivity(), "Plan saved", Toast.LENGTH_SHORT).show();


        clearViews();
        plansFlipper.setDisplayedChild(1);

    }


    private void deletePlan(Long trId, int position){
        Database db = new Database(getActivity().getBaseContext());
        db.deletePlan(trId);
        plans.remove(position);
        adapterPlans.notifyDataSetChanged();
    }

    private class PerformAsyncTask extends AsyncTask<Void, Void, Void> {
        private Activity activity;

        public PerformAsyncTask(Activity activity) {
            this.activity = activity;
        }

        protected void onPreExecute() {
            plansListView.setClickable(false);
        }

        @Override
        protected Void doInBackground(Void... unused) {
           getPlansFromDb(activity, true);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            plansListView.setClickable(true);
            if (adapterPlans!=null) adapterPlans.notifyDataSetChanged();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        showTextNoPlans();
    }

    @Override
    void onExit() {
        plansFlipper.setDisplayedChild(0);
    }
}
