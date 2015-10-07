package com.kostas.onlineHelp;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

public class DrawerItem {

    String ItemName;
    String title;
    String accountNum;
    int itemNameColor;
    int headerLayoutBackgroundColor;
    int headerLayoutVisibility;
    int itemLayoutVisibility;
    int itemLayoutBackgroundColor;
    int notifications;
    Drawable itemRightIconBackground;
    Typeface itemNameTypeface;
    Drawable profileIconImageDrawable;
    Drawable iconImageDrawable;
    Typeface titleTypeface;
    public DrawerItem(String ItemName,int itemNameColor, String accountNum,int headerLayoutBackgroundColor,int headerLayoutVisibility,
                      int itemLayoutVisibility,int itemLayoutBackgroundColor,Drawable itemRightIconBackground,
                      Typeface itemNameTypeface,Drawable profileIconImageDrawable,Drawable iconImageDrawable,
                      Typeface titleTypeface, int notifications) {
        this.ItemName = ItemName;
        this.itemNameColor = itemNameColor;
        this.accountNum = accountNum;
        this.notifications = notifications;
        this.headerLayoutBackgroundColor = headerLayoutBackgroundColor;
        this.headerLayoutVisibility=headerLayoutVisibility;
        this.itemLayoutVisibility=itemLayoutVisibility;
        this.itemLayoutBackgroundColor=itemLayoutBackgroundColor;
        this.itemRightIconBackground=itemRightIconBackground;
        this.itemNameTypeface=itemNameTypeface;
        this.profileIconImageDrawable=profileIconImageDrawable;
        this.iconImageDrawable=iconImageDrawable;
        this.titleTypeface=titleTypeface;
    }

    public int getItemNameColor() {
        return itemNameColor;
    }

    public void setItemNameColor(int itemNameColor) {
        this.itemNameColor = itemNameColor;
    }

    public String getItemName() {
        return ItemName;
    }

    public void setItemName(String itemName) {
        ItemName = itemName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAccountNum() {
        return accountNum;
    }

    public void setAccountNum(String accountNum) {
        this.accountNum = accountNum;
    }

    public int getHeaderLayoutBackgroundColor() {
        return headerLayoutBackgroundColor;
    }

    public void setHeaderLayoutBackgroundColor(int headerLayoutBackgroundColor) {
        this.headerLayoutBackgroundColor = headerLayoutBackgroundColor;
    }

    public int getHeaderLayoutVisibility() {
        return headerLayoutVisibility;
    }

    public void setHeaderLayoutVisibility(int headerLayoutVisibility) {
        this.headerLayoutVisibility = headerLayoutVisibility;
    }

    public int getItemLayoutVisibility() {
        return itemLayoutVisibility;
    }

    public void setItemLayoutVisibility(int itemLayoutVisibility) {
        this.itemLayoutVisibility = itemLayoutVisibility;
    }

    public int getItemLayoutBackgroundColor() {
        return itemLayoutBackgroundColor;
    }

    public void setItemLayoutBackgroundColor(int itemLayoutBackgroundColor) {
        this.itemLayoutBackgroundColor = itemLayoutBackgroundColor;
    }

    public Drawable getItemRightIconBackground() {
        return itemRightIconBackground;
    }

    public void setItemRightIconBackground(Drawable itemRightIconBackground) {
        this.itemRightIconBackground = itemRightIconBackground;
    }

    public Typeface getItemNameTypeface() {
        return itemNameTypeface;
    }

    public void setItemNameTypeface(Typeface itemNameTypeface) {
        this.itemNameTypeface = itemNameTypeface;
    }

    public Drawable getProfileIconImageDrawable() {
        return profileIconImageDrawable;
    }

    public void setProfileIconImageDrawable(Drawable profileIconImageDrawable) {
        this.profileIconImageDrawable = profileIconImageDrawable;
    }

    public Drawable getIconImageDrawable() {
        return iconImageDrawable;
    }

    public void setIconImageDrawable(Drawable iconImageDrawable) {
        this.iconImageDrawable = iconImageDrawable;
    }

    public Typeface getTitleTypeface() {
        return titleTypeface;
    }

    public void setTitleTypeface(Typeface titleTypeface) {
        this.titleTypeface = titleTypeface;
    }

    public int getNotifications() {
        return notifications;
    }

    public void setNotifications(int notifications) {
        this.notifications = notifications;
    }
}

