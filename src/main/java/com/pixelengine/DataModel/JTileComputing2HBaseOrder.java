package com.pixelengine.DataModel;
//2022-3-22



//离线瓦片计算，并输出到HBase的订单类
public class JTileComputing2HBaseOrder {
    public String jsfile;
    public String roi ;//this can be
                       // 1. empty string('')
                       // 2. roi2-ID(sys:1,user:2,...)
                       // 3. hseg.tlv filepath, use first character of '/' for identify filepath
    public double filldata ;
    public long dt ;
    public String sdui ;
    public int mpid_hpid ;
    public String out_htable;
    public long out_hcol ;
    public int out_hpidlen ;
    public int out_xylen ;
}
