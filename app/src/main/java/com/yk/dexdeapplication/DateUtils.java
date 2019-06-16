package com.yk.dexdeapplication;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");

    public void setCurrentDate(Date currentDate) {
        this.currentDate = simpleDateFormat.format(currentDate);
    }

    private String currentDate;

    public  String getCurrentDate(){
        return currentDate;
    }
}
