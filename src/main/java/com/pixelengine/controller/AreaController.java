package com.pixelengine.controller;


import com.pixelengine.DataModel.JRegion2;
import com.pixelengine.DataModel.Area;
import com.pixelengine.DataModel.JProductDataItem;
import com.pixelengine.DataModel.ROI;
import com.pixelengine.DataModel.RestResult;
import com.pixelengine.JRDBHelperForWebservice;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

@RestController
public class AreaController {

    //行政区划根节点列表
    @ResponseBody
    @RequestMapping(value="/area/root",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult areaRoot() throws IOException {
        RestResult rr = new RestResult() ;
        rr.setState(0);
        rr.setMessage("");

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        try{
            ArrayList<Area> datalist = rdb.rdbGetAreaList("0") ;
            rr.setData(datalist);
        }catch (Exception ex){
            rr.setState(1);
            rr.setMessage("exception");
        }
        return rr;
    }


    //行政区划按父节点代码查询子节点列表
    @ResponseBody
    @RequestMapping(value="/area/findByCode",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult findByCode( String parentCode ) throws IOException {
        RestResult rr = new RestResult() ;
        rr.setState(0);
        rr.setMessage("");
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        try{
            ArrayList<Area> datalist = rdb.rdbGetAreaList(parentCode) ;
            rr.setData(datalist);
        }catch (Exception ex){
            rr.setState(1);
            rr.setMessage("exception");
        }
        return rr;
    }



    //行政区划根节点列表
    @ResponseBody
    @RequestMapping(value="/area/root2",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult areaRoot2() throws IOException {
        RestResult rr = new RestResult() ;
        rr.setState(0);
        rr.setMessage("");

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        try{
            ArrayList<Area> datalist = rdb.rdbGetAreaList("0") ;
            ArrayList<ROI> roilist = new ArrayList<>() ;
            for(int i = 0 ; i < datalist.size(); ++ i ){
                roilist.add( ROI.convertArea2ROI(datalist.get(i))) ;
            }
            rr.setData(roilist);
        }catch (Exception ex){
            rr.setState(1);
            rr.setMessage("exception");
        }
        return rr;
    }


    //行政区划按父节点代码查询子节点列表
    @ResponseBody
    @RequestMapping(value="/area/findByCode2",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult findByCode2( String parentCode ) throws IOException {
        RestResult rr = new RestResult() ;
        rr.setState(0);
        rr.setMessage("");
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        try{
            ArrayList<Area> datalist = rdb.rdbGetAreaList(parentCode) ;
            ArrayList<ROI> roilist = new ArrayList<>() ;
            for(int i = 0 ; i < datalist.size(); ++ i ){
                roilist.add( ROI.convertArea2ROI(datalist.get(i))) ;
            }
            rr.setData(roilist);
        }catch (Exception ex){
            rr.setState(1);
            rr.setMessage("exception");
        }
        return rr;
    }
}
