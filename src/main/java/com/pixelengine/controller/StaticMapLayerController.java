package com.pixelengine.controller;
//静态地图图层信息

import com.pixelengine.DataModel.Area;
import com.pixelengine.DataModel.JStaticMapLayerProduct;
import com.pixelengine.DataModel.RestResult;
import com.pixelengine.JRDBHelperForWebservice;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

@RestController
public class StaticMapLayerController {

    @ResponseBody
    @RequestMapping(value="/staticmaplayer/all",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult all() throws IOException {
        RestResult rr = new RestResult() ;
        rr.setState(0);
        rr.setMessage("");

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();

        ArrayList<JStaticMapLayerProduct> data = rdb.rdbGetStaticMapLayer("") ;
        if( data==null ){
            rr.setState(1);
            rr.setMessage("获取静态产品图层失败");
        }else
        {
            rr.setState(0);
            rr.setMessage("");
            rr.setData(data);
        }
        return rr;
    }
    
}
