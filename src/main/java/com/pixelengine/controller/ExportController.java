package com.pixelengine.controller;
//离线任务数据导出
//update 2022-4-5
//update 2022-4-9 use pid replace pe in New2 mode


import com.google.gson.Gson;
import com.pixelengine.DAO.ZonalStatDAO;
import com.pixelengine.DTO.ZonalStatDTO;
import com.pixelengine.DataModel.*;
import com.pixelengine.JRDBHelperForWebservice;
import com.pixelengine.DataModel.JScript;
import com.pixelengine.DataModel.WConfig;
import com.pixelengine.tools.FileDirTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

@RestController
public class ExportController {
    @Autowired
    ZonalStatDAO dao ;

    @ResponseBody
    @RequestMapping(value="/offtask/export/new",method= RequestMethod.POST)
    @CrossOrigin(origins = "*")
    public RestResult exportNew( //老接口保留，不再维护，请使用new2方法
            String userid,
            String pid,
            String dt,
            String left,
            String right,
            String top,
            String bottom
    ) {//不再维护 2022-2-13 请使用new2
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
            String[] outdirArr = FileDirTool.checkAndMakeCurrentYearDateDir(WConfig.getSharedInstance().pedir,"export");
            ep.outfilename = outdirArr[0] + "export-u" + userid + "-" + FileDirTool.dateTimeString() + ".tif" ;
            ep.outfilenamedb = outdirArr[1] + "export-u" + userid + "-" + FileDirTool.dateTimeString() + ".tif" ;
            ep.zookeeper = WConfig.getSharedInstance().zookeeper ;
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



    // 2022-2-8
    @ResponseBody
    @RequestMapping(value="/offtask/export/new2",method= RequestMethod.POST)
    @CrossOrigin(origins = "*")
    public RestResult exportNew2(
            String uid,
            String mode , //pid or script
            String datetime,//yyyyMMddhhmmss 14digits
            String geojson, //relative path or {...} a real geojson string
            String pid,   //used in pe mode
            String sid,   //used in script mode
            String sdui,  //used in script mode , a real geojson string, if no used give a "null" String
            String fillvalue //number to fill
    ) {
        System.out.println("/offtask/export/new2") ;

        RestResult result = new RestResult();

        //write into file
        Date date = new Date();
        SimpleDateFormat datetimeFormat1 = new SimpleDateFormat("yyyyMMdd");
        String yyyyMMddStr = datetimeFormat1.format(date);
        SimpleDateFormat datetimeFormat2 = new SimpleDateFormat("HHmmss");
        String hhmmssStr = datetimeFormat2.format(date);
        String randStr = String.format("%04d",new Random().nextInt(9999)) ;
        String newFileNameNoExtension = hhmmssStr+"-"+randStr ;

        String exportDir = WConfig.getSharedInstance().pedir + "/export/" ;
        boolean dirok1 = FileDirTool.checkDirExistsOrCreate(exportDir) ;
        if( dirok1==false ){
            result.setState(9);
            result.setMessage("bad export/ dir.");
            return result ;
        }

        String exportYmdDir = WConfig.getSharedInstance().pedir + "/export/"+yyyyMMddStr+"/" ;
        boolean dirok2 = FileDirTool.checkDirExistsOrCreate(exportYmdDir) ;
        if( dirok2==false ){
            result.setState(9);
            result.setMessage("bad export/{yyyyMMdd} dir.");
            return result ;
        }

        String orderJsonFilepath = exportYmdDir + newFileNameNoExtension + ".json" ;
        String orderJsonRelFilepath = "export/" + yyyyMMddStr + "/" + newFileNameNoExtension + ".json" ;
        String resultRelFilepath = "export/" + yyyyMMddStr + "/" + newFileNameNoExtension + "-result.json" ;//2022-2-13

        JExportOrder theOrder = new JExportOrder() ;
        theOrder.datetime = Long.parseLong(datetime) ;
        theOrder.fillvalue = Double.parseDouble(fillvalue) ;
        theOrder.mode = mode ;//pid or script
        theOrder.pid = Integer.parseInt(pid) ;
        theOrder.sdui = sdui ;
        theOrder.resultRelFilepath = resultRelFilepath ;

        //geojson
        {
            if( geojson.length() == 0){
                result.setState(9);
                result.setMessage("geojson zero length.");
                return result ;
            }
            if( geojson.getBytes()[0] == '{' ){
                //write into geojson directly
                String geojsonfilepath = exportYmdDir + newFileNameNoExtension + ".geojson" ;//absolute path
                try{
                    OutputStream outputStream = new FileOutputStream(geojsonfilepath);
                    outputStream.write(geojson.getBytes());
                    outputStream.close();
                    theOrder.geojsonRelFilepath = "export/"+yyyyMMddStr+"/"+ newFileNameNoExtension + ".geojson";//relative path
                }catch (Exception ex ){
                    result.setState(9);
                    result.setMessage("write geojson failed.");
                    return result ;
                }
            }else{
                //use relative path
                theOrder.geojsonRelFilepath =  geojson ;
            }
        }

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
        //script
        if( mode.compareTo("script") == 0 ){
            JScript scriptObj = rdb.rdbGetScript( Integer.parseInt(sid) ) ;
            theOrder.scriptRelFilepath = scriptObj.jsfile ;
        }else{
            //pid mode ,use a system script for export
            JProduct pdt = rdb.rdbGetProductForAPI(Integer.parseInt(pid)) ;
            if( pdt==null ){
                result.setState(9);
                result.setMessage("no product for pid "+pid);
            }
            String sysPeScript =
                    "function main(){ "
                    +"let ds=pe.Dataset('" + pdt.name+ "',"+ datetime + ");"
                    +"return ds;"
                    +"}" ;
            //write into temp js file
            String tempRelJsFilepath = "export/" + yyyyMMddStr + "/" + newFileNameNoExtension + "_pe.js" ;
            String tempJsFilepath = exportYmdDir + newFileNameNoExtension + "_pe.js" ;//absolute path
            try{
                OutputStream outputStream = new FileOutputStream(tempJsFilepath);
                outputStream.write(sysPeScript.getBytes());
                outputStream.close();
                theOrder.scriptRelFilepath = tempRelJsFilepath ;
            }catch (Exception ex ){
                result.setState(9);
                result.setMessage("write temp pe js file failed.");
                return result ;
            }
        }

        Gson gson = new Gson() ;
        String theOrderJsonText = gson.toJson( theOrder , JExportOrder.class) ;

        //write order into file
        try{
            OutputStream outputStream = new FileOutputStream(orderJsonFilepath);
            outputStream.write(theOrderJsonText.getBytes());
            outputStream.close();
        }catch (Exception ex ){
            result.setState(9);
            result.setMessage("write order failed.");
            return result ;
        }

        //write order json into file ok, then write into mysql.
        int exportMode = 5 ;
        int ofid =  rdb.rdbNewOffTask(  Integer.parseInt(uid) , exportMode , orderJsonRelFilepath,
                resultRelFilepath ) ;
        if( ofid>0 ){
            //这里添加zeromq调用 这里传递两个值 一个order主键和order json文件的相对路径
            JOfftaskOrderMsg msg=new JOfftaskOrderMsg() ;
            msg.ofid = ofid ;
            msg.mode = 5 ;//2022-4-5
            msg.orderRelFilepath = orderJsonRelFilepath ;
            JOfftaskOrderSender.getSharedInstance().send(msg);
            result.setState(0);
            result.setMessage("");
            result.setData("{\"ofid\":" + String.valueOf(ofid) + "}");
            return result ;
        }
        else{
            result.setState(9);
            result.setMessage("insert db failed.");
            return result ;
        }
    }






}
