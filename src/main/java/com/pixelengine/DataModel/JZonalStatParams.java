package com.pixelengine.DataModel;

import com.google.gson.Gson;

public class JZonalStatParams {
    public String rtype;
    public Long rid ;
    public int pid,bandindex ;
    public double vmin,vmax;
    public Long dt ;//datetime
    //extra params by produt info
    public String regfile ;
    public int zlevel ;
    public String hTableName;
    public Integer hPid ;
    public int bsqIndex ;
    public int dataType ;
    public String hFamily ;
    public int hpidblen ;//hbase pid byte length
    public int yxblen ; //hbase tile yx byte length

    public String toJson() {
        Gson gson = new Gson();
        String jsonstr = gson.toJson(this);
        return jsonstr ;
    }
    public static JZonalStatParams fromJson(String jsonstr){
        Gson gson = new Gson();
        JZonalStatParams res = gson.fromJson(jsonstr,JZonalStatParams.class) ;
        return res ;
    }
}
