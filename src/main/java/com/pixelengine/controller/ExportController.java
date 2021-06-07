package com.pixelengine.controller;
//离线任务数据导出


import com.google.gson.Gson;
import com.pixelengine.DAO.ZonalStatDAO;
import com.pixelengine.DTO.ZonalStatDTO;
import com.pixelengine.DataModel.JExportParams;
import com.pixelengine.DataModel.JProduct;
import com.pixelengine.DataModel.JProductDisplay;
import com.pixelengine.DataModel.RestResult;
import com.pixelengine.JRDBHelperForWebservice;
import com.pixelengine.WConfig;
import com.pixelengine.tools.FileDirTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.jar.JarException;

@RestController
public class ExportController {
    @Autowired
    ZonalStatDAO dao ;

    @ResponseBody
    @RequestMapping(value="/offtask/export/new",method= RequestMethod.POST)
    @CrossOrigin(origins = "*")
    public RestResult exportNew(
            String userid,
            String pid,
            String dt,
            String left,
            String right,
            String top,
            String bottom
    ) {
        System.out.println("/offtask/export/new") ;

        RestResult result = new RestResult() ;
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;

        JProduct product = rdb.rdbGetProductForAPI( Integer.parseInt(pid)) ;
        if( product==null )
        {
            result.setState(1);
            result.setMessage("no product for pid:" + pid);
            return result ;
        }
        else
        {
            JExportParams ep = new JExportParams() ;
            ep.inpid = Integer.parseInt(pid) ;

            String useTag = product.name ;
            JProductDisplay pdtDisplay = rdb.rdbGetProductDisplayInfo(ep.inpid) ;
            if( pdtDisplay != null && pdtDisplay.productname.compareTo("")!=0 )
            {
                useTag = pdtDisplay.productname ;
            }

            ep.dt = Long.parseLong(dt) ;
            ep.htable = product.hbaseTable.hTableName ;
            ep.hfami = product.hbaseTable.hFamily ;
            ep.hpid = product.bandList.get(0).hPid ;
            ep.hpidblen = product.hbaseTable.hPidByteNum ;
            ep.yxblen = product.hbaseTable.hYXByteNum ;
            ep.left = Double.valueOf(left) ;
            ep.right = Double.valueOf(right) ;
            ep.top = Double.valueOf(top) ;
            ep.bottom = Double.valueOf(bottom) ;
            ep.level = product.maxZoom ;
            ep.filldata = (int) product.bandList.get(0).noData;
            String[] outdirArr = FileDirTool.checkAndMakeCurrentYearDateDir(WConfig.sharedConfig.pedir,"export");
            ep.outfilename = outdirArr[0] + "export-u" + userid + "-" + FileDirTool.dateTimeString() + ".tif" ;
            ep.outfilenamedb = outdirArr[1] + "export-u" + userid + "-" + FileDirTool.dateTimeString() + ".tif" ;
            ep.zookeeper = WConfig.sharedConfig.zookeeper ;
            ep.datatype = product.dataType ;

            Gson gson = new Gson() ;
            String paramsJsonText = gson.toJson(ep , JExportParams.class) ;

            ZonalStatDTO task =new ZonalStatDTO() ;
            task.setContent(paramsJsonText);
            task.setCreatetime( new Date());
            task.setUpdatetime( new Date());
            task.setMessage("");
            task.setResult("");
            task.setStatus(0);
            task.setTag(useTag);
            task.setUid(Long.parseLong(userid));
            task.setMode(5);//0-zs , 1-sk , 2-ls , 4-composite , 5-export
            ZonalStatDTO newtask = dao.save(task) ;

            result.setState(0);
            result.setMessage("");
            result.setData(newtask);

            return result ;
        }
    }






}
