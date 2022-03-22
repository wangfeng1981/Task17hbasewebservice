package com.pixelengine.DataModel;
//2022-01-01

import java.util.Date;

/// second version ROI
/// support both system roi and user roi
public class JRoi2 {
    public int rid = 0 ;//both
    public String name ;//both
    public String geojson ;//both

    public int rcid = 0 ;//sys
    public String name2;//sys
    public String shp;//user
    public int uid;//user
    public Date ctime;//user

}
