package com.pixelengine.controller;
//2022-7-8


import com.pixelengine.DataModel.Area;
import com.pixelengine.DataModel.JProduct;
import com.pixelengine.DataModel.JProductDataItem;
import com.pixelengine.DataModel.RestResult;
import com.pixelengine.JRDBHelperForWebservice;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;

@RestController
public class DatetimeController {

    //获取小于等于输入时间的记录
    @ResponseBody
    @RequestMapping(value="/datetime/lqnearbypid",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult lqnearByPid(int pid,long datetime) throws IOException {
        RestResult rr = new RestResult() ;
        rr.setState(0);
        rr.setMessage("") ;

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        JProduct pdt = rdb.rdbGetProductForAPI(pid) ;
        if( pdt==null ){
            rr.setState(1);
            rr.setMessage("invalid product id.");
        }else{
            JProductDataItem di = rdb.rdbGetLowerEqualNearestDt0(pid,datetime,pdt.timeType) ;
            if( di==null ){
                rr.setState(2);
                rr.setMessage("not datetime lower equal than " + datetime);
            }else{
                rr.setState(0);
                rr.setData(di);
            }
        }
        return rr;
    }

    //获取小于等于输入时间的记录
    @ResponseBody
    @RequestMapping(value="/datetime/lqnearbypname",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult lqnearByPid(String pname,long datetime) throws IOException {
        RestResult rr = new RestResult() ;
        rr.setState(0);
        rr.setMessage("") ;

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        JProduct pdt = rdb.rdbGetProductInfoByName(pname);
        if( pdt==null ){
            rr.setState(1);
            rr.setMessage("invalid product name.");
        }else{
            JProductDataItem di = rdb.rdbGetLowerEqualNearestDt0(pdt.pid,datetime,pdt.timeType) ;
            if( di==null ){
                rr.setState(2);
                rr.setMessage("not datetime lower equal than " + datetime);
            }else{
                rr.setState(0);
                rr.setData(di);
            }
        }
        return rr;
    }
}
