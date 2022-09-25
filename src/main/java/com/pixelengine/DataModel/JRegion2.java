package com.pixelengine.DataModel;


import java.util.Date;

//replace class RegionDTO {
//
public class JRegion2 {
    private Long rid;
    private String name;
    private String shp;
    private String geojson;
    private Integer uid;
    private Date createtime;
    private Date updatetime;

    public Long getRid() {
        return rid;
    }

    public void setRid(Long rid) {
        this.rid = rid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShp() {
        return shp;
    }

    public void setShp(String shp) {
        this.shp = shp;
    }

    public String getGeojson() {
        return geojson;
    }

    public void setGeojson(String geojson) {
        this.geojson = geojson;
    }

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public Date getCreatetime() {
        return createtime;
    }

    public void setCreatetime(Date createtime) {
        this.createtime = createtime;
    }

    public Date getUpdatetime() {
        return updatetime;
    }

    public void setUpdatetime(Date updatetime) {
        this.updatetime = updatetime;
    }

    //
    public ROI convert2ROI(){
        ROI roi = new ROI () ;
        roi.rid = Math.toIntExact( this.getRid() ) ;
        roi.shp = this.shp ;
        roi.uid = this.uid ;
        roi.children = 0 ;
        roi.code = "0" ;
        roi.parentCode = "0" ;
        roi.geojson = this.geojson  ;
        roi.name = this.name ;
        roi.rtype = "roi" ;
        roi.createtime = this.createtime ;
        roi.updatetime = this.updatetime ;
        return roi ;
    }
}