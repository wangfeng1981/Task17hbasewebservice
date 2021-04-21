package com.pixelengine.DataModel;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

public class ROI {
    //
    public String rtype = "roi";// roi / area
    public int children = 0;//是否有子节点，0-没有，1-有
    public String code = "0" ;//仅针对预定于区域
    public String parentCode="0" ;//仅针对预定于区域
    public int rid ; //主键
    public String name ;
    public String shp ;
    public String geojson ;
    public int uid ;
    public Date createtime ;
    public Date updatetime;

}