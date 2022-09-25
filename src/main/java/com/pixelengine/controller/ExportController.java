package com.pixelengine.controller;
//离线任务数据导出
//update 2022-4-5
//update 2022-4-9 use pid replace pe in New2 mode
//2022-7-27

import com.google.gson.Gson;
import com.pixelengine.DataModel.JZonalStat2;
//import com.pixelengine.DTO.ZonalStatDTO;
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
        RestResult result=new RestResult();
        result.setState(1);
        result.setMessage("deprecated");
        return result ;

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
            String sysPeScript = JSharedScriptTemplates.scriptTemplate_name;
            sysPeScript=sysPeScript.replace("{{{name}}}",pdt.name);//2022-7-27
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
            boolean sendok = JOfftaskOrderSender.getSharedInstance().send(msg);
            if(sendok==false){
                rdb.updateOfftaskState(ofid,3);
                result.setState(13);
                result.setMessage("0mq failed to send.");
                return result ;
            }
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
