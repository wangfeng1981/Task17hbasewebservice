package com.pixelengine.DTO;



import com.pixelengine.DataModel.ROI;

import javax.persistence.*;
import java.util.Date;

/**
 * @author zyp
 * @date 2021/1/13
 */
////deprecated 2022-9-14
//@Entity
//@Table(name="tbregion")
//public class RegionDTO {
//    //
//
//    @Id
//    @GeneratedValue(strategy= GenerationType.IDENTITY)
//    @Column(name = "rid")
//    private Long rid;
//
//    @Column(name = "name")
//    private String name;
//
//    @Column(name = "shp")
//    private String shp;
//
//    @Column(name = "geojson")
//    private String geojson;
//
//    @Column(name = "uid")
//    private Integer uid;
//
//    @Column(name = "createtime")
//    private Date createtime;
//
//    @Column(name = "updatetime")
//    private Date updatetime;
//
//    public Long getRid() {
//        return rid;
//    }
//
//    public void setRid(Long rid) {
//        this.rid = rid;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public String getShp() {
//        return shp;
//    }
//
//    public void setShp(String shp) {
//        this.shp = shp;
//    }
//
//    public String getGeojson() {
//        return geojson;
//    }
//
//    public void setGeojson(String geojson) {
//        this.geojson = geojson;
//    }
//
//    public Integer getUid() {
//        return uid;
//    }
//
//    public void setUid(Integer uid) {
//        this.uid = uid;
//    }
//
//    public Date getCreatetime() {
//        return createtime;
//    }
//
//    public void setCreatetime(Date createtime) {
//        this.createtime = createtime;
//    }
//
//    public Date getUpdatetime() {
//        return updatetime;
//    }
//
//    public void setUpdatetime(Date updatetime) {
//        this.updatetime = updatetime;
//    }
//
//    //
//    public ROI convert2ROI(){
//        ROI roi = new ROI () ;
//        roi.rid = Math.toIntExact( this.getRid() ) ;
//        roi.shp = this.shp ;
//        roi.uid = this.uid ;
//        roi.children = 0 ;
//        roi.code = "0" ;
//        roi.parentCode = "0" ;
//        roi.geojson = this.geojson  ;
//        roi.name = this.name ;
//        roi.rtype = "roi" ;
//        roi.createtime = this.createtime ;
//        roi.updatetime = this.updatetime ;
//        return roi ;
//    }
//}
