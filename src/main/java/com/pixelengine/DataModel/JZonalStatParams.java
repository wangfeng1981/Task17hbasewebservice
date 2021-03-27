package com.pixelengine.DataModel;

import com.google.gson.Gson;

public class JZonalStatParams {
    public String rtype;
    public Long rid ;
    public int pid,bandindex ;
    public double vmin,vmax;
    public Long dt ;
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
