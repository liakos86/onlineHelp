package com.kostas.onlineHelp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;


public class CustomDrawerAdapter extends ArrayAdapter<DrawerItem> {
 
    Context context;
    List<DrawerItem> drawerItemList;
    int layoutResID;

    public CustomDrawerAdapter(Context context, int layoutResourceID, List<DrawerItem> listItems) {
        super(context, layoutResourceID, listItems);
        this.context = context;
        drawerItemList = listItems;
        layoutResID = layoutResourceID;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        switch (position) {

//            case BaseDrawer.EDIT_PROFILE_POSITION:
//                if (id!=null&&id==-1) return true;
//                return context.getResources().getBoolean(R.bool.is_edit_profile_enabled);
//            case BaseDrawer.MY_BILL_POSITION:
//                return context.getResources().getBoolean(R.bool.is_my_bill_enabled);
//            case BaseDrawer.MY_USAGE_POSITION:
//                if (id!=null&&id==-1) return false;
//                return context.getResources().getBoolean(R.bool.is_my_usage_enabled);
//            case BaseDrawer.MY_PLAN_POSITION:
//                return context.getResources().getBoolean(R.bool.is_my_plan_enabled);
//            case BaseDrawer.VODAFONE_OFFERS_POSITION:
//                return context.getResources().getBoolean(R.bool.is_vodafone_offers_enabled);
//            case BaseDrawer.INOFFICE_POSITION:
//                return context.getResources().getBoolean(R.bool.is_inoffice_enabled);
//            case BaseDrawer.VODAFONE_UCO_POSITION:
//                return context.getResources().getBoolean(R.bool.is_vodafone_uco_enabled);
//            case BaseDrawer.TRACKING_POSITION:
//                return context.getResources().getBoolean(R.bool.is_tracking_enabled);
//            case BaseDrawer.ABROAD_POSITION:
//                return context.getResources().getBoolean(R.bool.is_abroad_enabled);
//            case BaseDrawer.STORE_LOCATOR_POSITION:
//                return context.getResources().getBoolean(R.bool.is_store_locator_enabled);
//            case BaseDrawer.NETWORK_COVERAGE_POSITION:
//                return context.getResources().getBoolean(R.bool.is_network_coverage_enabled);
//               return context.getResources().getBoolean(R.bool.is_log_out_enabled);
        }
        return  !((ExtApplication)getContext().getApplicationContext()).isInRunningMode();
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        DrawerItemHolder drawerHolder = null;
        ExtApplication application = (ExtApplication) getContext().getApplicationContext();

        if(view==null || !(view.getTag() instanceof DrawerItemHolder)){
            LayoutInflater mInflater = LayoutInflater.from(context);
            view = mInflater.inflate(layoutResID, null);
            drawerHolder = new DrawerItemHolder();
            drawerHolder.itemName = (TextView) view.findViewById(R.id.drawer_itemName);
            drawerHolder.icon = (ImageView) view.findViewById(R.id.drawer_icon);
            drawerHolder.profileIcon = (CircularImageView) view.findViewById(R.id.profile_icon);
            drawerHolder.title = (TextView) view.findViewById(R.id.drawerTitle);
            drawerHolder.accountNum = (TextView) view.findViewById(R.id.accountNum);
            drawerHolder.headerLayout = (LinearLayout) view.findViewById(R.id.headerLayout);
            drawerHolder.itemLayout = (LinearLayout) view.findViewById(R.id.itemLayout);
            drawerHolder.itemRightIcon = (TextView) view.findViewById(R.id.item_right_icon);
            view.setTag(drawerHolder);
        } else {
            drawerHolder = (DrawerItemHolder) view.getTag();
        }

        DrawerItem dItem = (DrawerItem) drawerItemList.get(position);

            drawerHolder.headerLayout.setBackgroundColor(dItem.headerLayoutBackgroundColor);
            drawerHolder.headerLayout.setVisibility(dItem.headerLayoutVisibility);
            drawerHolder.itemLayout.setVisibility(dItem.itemLayoutVisibility);

            drawerHolder.profileIcon.setImageDrawable(dItem.profileIconImageDrawable);


        drawerHolder.icon.setImageDrawable(dItem.iconImageDrawable);
        drawerHolder.title.setText(dItem.getItemName());

        drawerHolder.title.setTypeface(dItem.getTitleTypeface());
        drawerHolder.accountNum.setText(dItem.getAccountNum());

        if(null!=dItem.getItemRightIconBackground()){
            drawerHolder.itemRightIcon.setVisibility(View.VISIBLE);
            if (Build.VERSION.SDK_INT >= 16) {
                drawerHolder.itemRightIcon.setBackground(dItem.getItemRightIconBackground());
            } else {
                drawerHolder.itemRightIcon.setBackgroundDrawable(dItem.getItemRightIconBackground());
            }
        }else {
            drawerHolder.itemRightIcon.setVisibility(View.INVISIBLE);
            drawerHolder.itemRightIcon.setBackgroundDrawable(null);
        }

            //we will prefetch the notifications from the db to set the num
//            if (position == BaseDrawer.MY_NOTIFICATIONS_POSITION) {
//
//                Long id = null;
//                if(application!=null){
//                    id = application.getUserID();
//                }
//                if (id != null && id==-1L){
//                    drawerHolder.itemRightIcon.setText("");
//                } else if (getContext().getClass().toString().contains("ActMyNotif")&&id!=null&&id!=-1) {
//                    drawerHolder.itemRightIcon.setText("0");
//                }else{
//                    drawerHolder.itemRightIcon.setText(String.valueOf(dItem.getNotifications()));
//                }
//            }else {
                drawerHolder.itemRightIcon.setText(dItem.getAccountNum());
//            }

            drawerHolder.itemName.setTypeface(dItem.getItemNameTypeface());

            drawerHolder.itemLayout.setBackgroundColor(dItem.getItemLayoutBackgroundColor());

            drawerHolder.itemName.setText(dItem.getItemName());



//        if (dItem.getItemNameColor()== getContext().getResources().getColor(R.color.death_grey)) {
//            drawerHolder.itemName.setTextColor(dItem.itemNameColor);
//
//        }

//        if(application!=null){
//            int positionSelected = application.getPosition();
//            if(position == positionSelected){
//                view.performClick();
//            }
//        }

        return view;
    }

    private static class DrawerItemHolder {
        TextView itemName;
        TextView title;
        TextView accountNum;
        TextView itemRightIcon;
        ImageView icon;
        CircularImageView profileIcon;
        LinearLayout headerLayout;
        LinearLayout itemLayout;

    }
}