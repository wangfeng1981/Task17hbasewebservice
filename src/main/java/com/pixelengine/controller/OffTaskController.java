package com.pixelengine.controller;
//这个还没想好怎么改 2021-4-1 这个不再使用，请使用ZonalStatController，或者参考ZonalStatController修改。
//2022-4-5 使用该Controller 作为新版 离线任务 区域统计，序列分析，数据合成 接口

import com.google.gson.Gson;
import com.pixelengine.DTO.ZonalStatDTO;
import com.pixelengine.DataModel.*;
import com.pixelengine.JRDBHelperForWebservice;
import com.pixelengine.tools.FileDirTool;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/offtask")
public class OffTaskController {

    //新建数据合成任务，合成结果写入HBase
    @CrossOrigin(origins = "*")
    @PostMapping("/composite/new2")
    @ResponseBody
    public RestResult compositeNew2(
                                String uid,
                                String dsname,
                                String startdt,
                                String stopdt ,
                                String roiid ,   //optional
                                String roigeojson,//optional
                                String validmin,
                                String validmax,
                                String filldata,
                                String cmethod // 1-min 2-max 3-ave 4-sum
    ) {
        System.out.println("compositeNew2");

        RestResult result = new RestResult();

        //write into file
        Date date = new Date();
        SimpleDateFormat datetimeFormat1 = new SimpleDateFormat("yyyyMMdd");
        String yyyyMMddStr = datetimeFormat1.format(date);
        SimpleDateFormat datetimeFormat2 = new SimpleDateFormat("HHmmss");
        String hhmmssStr = datetimeFormat2.format(date);
        String randStr = String.format("%04d",new Random().nextInt(9999)) ;
        String newFileNameNoExtension = "tc2hb-" + hhmmssStr+"-"+randStr ;//tc2hb for tilecomputing to HBase

        String outdir = WConfig.getSharedInstance().pedir + "/offtask/" ;
        boolean dirok1 = FileDirTool.checkDirExistsOrCreate(outdir) ;
        if( dirok1==false ){
            result.setState(9);
            result.setMessage("bad  outdir.");
            return result ;
        }

        String outYmdDir = WConfig.getSharedInstance().pedir + "/offtask/"+yyyyMMddStr+"/" ;
        boolean dirok2 = FileDirTool.checkDirExistsOrCreate(outYmdDir) ;
        if( dirok2==false ){
            result.setState(9);
            result.setMessage("bad outYmdDir.");
            return result ;
        }

        String absJsFilepath = outYmdDir + newFileNameNoExtension + ".js" ;
        {
            //构造数据合成js脚本文件，并写入硬盘，后面附加到order json文件中
            String outtypestr = "" ;
            if( cmethod.equals('3') || cmethod.equals('4') ) outtypestr=",6" ;
            String composeJsText = "function main(){" +
                    "let dtcs=pe.RemoteBuildDtCollections('"+dsname+"',"+startdt+",1,"+stopdt+",1,'',0,0,0,0,0);" +
                    "let dscs=pe.DatasetCollections('"+dsname+"',dtcs);" +
                    "let ds=pe.CompositeDsCollections(dscs,"+cmethod+
                    ","+validmin+","+validmax+","+filldata+outtypestr+");" +
                    "return ds;"+
                    "}" ;
            if( FileDirTool.writeToTextFile( absJsFilepath , composeJsText) == false )
            {
                result.setState(9);
                result.setMessage("bad js file.");
                return result ;
            }
        }

        String absOrderJsonFilepath = outYmdDir + newFileNameNoExtension + ".json" ;
        String absTempRoiGeoJsonFilepath = outYmdDir + newFileNameNoExtension + "-roi.geojson" ;
        String absTempRoiTlvFilepath = outYmdDir + newFileNameNoExtension + "-roi.hseg.tlv" ;
        String orderJsonRelFilepath = "offtask/" + yyyyMMddStr + "/" + newFileNameNoExtension + ".json" ;
        String resultRelFilepath = "offtask/" + yyyyMMddStr + "/" + newFileNameNoExtension + "-result.json" ;

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
        //在tbproduct新建一个记录
        String uuidDsname = UUID.randomUUID().toString() ;
        System.out.println("debug uuidDsname:"+uuidDsname);
        int newpdtid = rdb.rdbNewEmptyUserProduct(uuidDsname,Integer.valueOf(uid));
        if( newpdtid<=0 ){
            result.setState(9);
            result.setMessage("bad new product id.");
            return result ;
        }

        //构造ROI数据
        String roiStr = "" ;
        if( roiid.equals("") == false ){
            roiStr = roiid ;
        }else if( roigeojson.equals("") ==false ){
            //write geojson into file
            if( FileDirTool.writeToTextFile(absTempRoiGeoJsonFilepath,roigeojson) ==false ) {
                result.setState(9);
                result.setMessage("bad writing geojson.");
                return result ;
            }
            //convert geojson into tlv
            String tlvcmd = WConfig.getSharedInstance().shpgeojson2hsegtlv
                    + " " + absTempRoiGeoJsonFilepath + " "+ absTempRoiTlvFilepath;
            try{
                Runtime run  = Runtime.getRuntime();
                Process proc = run.exec(tlvcmd);
                proc.waitFor() ;
                File tlvfile = new File(absTempRoiTlvFilepath) ;
                if( tlvfile.exists()==false ){
                    result.setState(9);
                    result.setMessage("bad tlv file.");
                    return result ;
                }
                roiStr = absTempRoiTlvFilepath ;
            }catch (Exception ex1)
            {
                result.setState(9);
                result.setMessage("bad converting tlv.");
                return result ;
            }
        }

        JTileComputing2HBaseOrder orderObject = new JTileComputing2HBaseOrder() ;
        orderObject.mpid_hpid = newpdtid;//
        orderObject.roi = roiStr ;
        orderObject.jsfile = absJsFilepath ;
        orderObject.dt = 1 ;
        orderObject.filldata = Double.valueOf(filldata);
        orderObject.sdui = "" ;
        orderObject.out_hcol = 1 ;
        orderObject.out_hpidlen = WConfig.getSharedInstance().userhpidblen ;
        orderObject.out_htable = WConfig.getSharedInstance().userhtable ;
        orderObject.out_xylen = WConfig.getSharedInstance().useryxblen ;

        boolean writeOrderJsonOk = FileDirTool.writeToTextFile(absOrderJsonFilepath ,
                new Gson().toJson(orderObject,JTileComputing2HBaseOrder.class)) ;
        if( writeOrderJsonOk==false ){
            result.setState(9);
            result.setMessage("failed write order json.");
            return result ;
        }

        //写入离线任务到mysql
        int mode = 4 ;//0-zonalstat 1-skSerial 2-lsSerial 4-tcHbase 5-export
        int ofid =  rdb.rdbNewOffTask(  Integer.parseInt(uid) , mode , orderJsonRelFilepath,
                resultRelFilepath ) ;
        if( ofid>0 ){
            //这里添加zeromq调用 这里传递两个值 一个order主键和order json文件的相对路径
            JOfftaskOrderMsg msg=new JOfftaskOrderMsg() ;
            msg.ofid = ofid ;
            msg.mode = 4 ;
            msg.orderRelFilepath = orderJsonRelFilepath ;
            JOfftaskOrderSender.getSharedInstance().send(msg);
            result.setState(0);
            result.setMessage("");
            result.setData("{\"ofid\":" + String.valueOf(ofid) + "}");
            return result ;
        }
        else{
            result.setState(9);
            result.setMessage("bad mysql offtask record.");
            return result ;
        }

    }



    //新建区域统计任务，
    @CrossOrigin(origins = "*")
    @PostMapping("/stat/new2")
    @ResponseBody
    public RestResult statNew2(
            String uid,
            String dsname,
            String sid ,
            String roiid ,   //roiid or roigeojson required
            String roigeojson,
            String validmin,
            String validmax,
            String datetime, //dsname 生效时为具体数据日期，反之为前端显示日期
            String sdui      //仅在sid生效时有用
    ) {
        System.out.println("statNew2");
        RestResult result = new RestResult();

        //write into file
        Date date = new Date();
        SimpleDateFormat datetimeFormat1 = new SimpleDateFormat("yyyyMMdd");
        String yyyyMMddStr = datetimeFormat1.format(date);
        SimpleDateFormat datetimeFormat2 = new SimpleDateFormat("HHmmss");
        String hhmmssStr = datetimeFormat2.format(date);
        String randStr = String.format("%04d",new Random().nextInt(9999)) ;
        String newFileNameNoExtension = "zs-" + hhmmssStr+"-"+randStr ;//zs for zonal statistic

        String outdir = WConfig.getSharedInstance().pedir + "/offtask/" ;
        boolean dirok1 = FileDirTool.checkDirExistsOrCreate(outdir) ;
        if( dirok1==false ){
            result.setState(9);
            result.setMessage("bad  outdir.");
            return result ;
        }

        String outYmdDir = WConfig.getSharedInstance().pedir + "/offtask/"+yyyyMMddStr+"/" ;
        boolean dirok2 = FileDirTool.checkDirExistsOrCreate(outYmdDir) ;
        if( dirok2==false ){
            result.setState(9);
            result.setMessage("bad outYmdDir.");
            return result ;
        }

        String absOrderJsonFilepath = outYmdDir + newFileNameNoExtension + ".json" ;
        String absTempRoiGeoJsonFilepath = outYmdDir + newFileNameNoExtension + "-roi.geojson" ;
        String absTempRoiTlvFilepath = outYmdDir + newFileNameNoExtension + "-roi.hseg.tlv" ;
        String orderJsonRelFilepath = "offtask/" + yyyyMMddStr + "/" + newFileNameNoExtension + ".json" ;
        String resultRelFilepath = "offtask/" + yyyyMMddStr + "/" + newFileNameNoExtension + "-result.json" ;

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;

        JTileComputingStatisticOrder orderObj = new JTileComputingStatisticOrder() ;
        orderObj.dsname = dsname ;
        orderObj.jsfile = "" ;//absolute js file path
        if( sid.equals("0") == false )
        {
            JScript scriptObj = rdb.rdbGetScript(Integer.valueOf(sid)) ;
            String absJsFilepath = WConfig.getSharedInstance().pedir + scriptObj.jsfile ;
            orderObj.jsfile = absJsFilepath ;
        }
        orderObj.dt = Long.valueOf(datetime) ;
        orderObj.filldata = Double.valueOf(validmin) - 1 ;
        orderObj.validMinInc = Double.valueOf(validmin) ;
        orderObj.validMaxInc = Double.valueOf(validmax) ;
        orderObj.sdui = sdui ;

        if( roiid.equals("") && roigeojson.equals("") ){
            //roi must required
            result.setState(9);
            result.setMessage("no roi");
            return result ;
        }

        //构造ROI数据
        String roiStr = "" ;
        if( roiid.equals("") == false ){
            roiStr = roiid ;
        }else if( roigeojson.equals("") ==false ){
            //write geojson into file
            if( FileDirTool.writeToTextFile(absTempRoiGeoJsonFilepath,roigeojson) ==false ) {
                result.setState(9);
                result.setMessage("bad writing geojson.");
                return result ;
            }
            //convert geojson into tlv
            String tlvcmd = WConfig.getSharedInstance().shpgeojson2hsegtlv
                    + " " + absTempRoiGeoJsonFilepath + " "+ absTempRoiTlvFilepath;
            try{
                Runtime run  = Runtime.getRuntime();
                Process proc = run.exec(tlvcmd);
                proc.waitFor() ;
                File tlvfile = new File(absTempRoiTlvFilepath) ;
                if( tlvfile.exists()==false ){
                    result.setState(9);
                    result.setMessage("bad tlv file.");
                    return result ;
                }
                roiStr = absTempRoiTlvFilepath ;
            }catch (Exception ex1)
            {
                result.setState(9);
                result.setMessage("bad converting tlv.");
                return result ;
            }
        }

        orderObj.roi =roiStr ;

        boolean writeOrderJsonOk = FileDirTool.writeToTextFile(absOrderJsonFilepath ,
                new Gson().toJson(orderObj,JTileComputingStatisticOrder.class)) ;
        if( writeOrderJsonOk==false ){
            result.setState(9);
            result.setMessage("failed write order json.");
            return result ;
        }

        //写入离线任务到mysql
        int mode = 0 ;//0-zonalstat 1-skSerial 2-lsSerial 4-tcHbase 5-export
        int ofid =  rdb.rdbNewOffTask(  Integer.parseInt(uid) , mode , orderJsonRelFilepath,
                resultRelFilepath ) ;
        if( ofid>0 ){
            //这里添加zeromq调用 这里传递两个值 一个order主键和order json文件的相对路径
            JOfftaskOrderMsg msg=new JOfftaskOrderMsg() ;
            msg.ofid = ofid ;
            msg.mode = mode ;
            msg.orderRelFilepath = orderJsonRelFilepath ;
            JOfftaskOrderSender.getSharedInstance().send(msg);
            result.setState(0);
            result.setMessage("");
            result.setData("{\"ofid\":" + String.valueOf(ofid) + "}");
            return result ;
        }
        else{
            result.setState(9);
            result.setMessage("bad mysql offtask record.");
            return result ;
        }
    }



    //新建序列分析
    @CrossOrigin(origins = "*")
    @PostMapping("/serial/new2")
    @ResponseBody
    public RestResult serialNew2(
            String uid,
            String dsname,
            String repeattype,
            String wholestartdt,
            String wholestopdt ,
            String repeatstartdt,
            String repeatstopdt,
            String repeatstopnextyear, //0,1
            String roiid ,   //roi required
            String roigeojson,
            String validmin,
            String validmax,
            String filldata,
            String cmethod // 1-min 2-max 3-ave 4-sum
    ) {
        System.out.println("serialNew2");

        if( repeattype.equals("n")) repeattype = "" ;//2022-4-5

        RestResult result = new RestResult();

        //write into file
        Date date = new Date();
        SimpleDateFormat datetimeFormat1 = new SimpleDateFormat("yyyyMMdd");
        String yyyyMMddStr = datetimeFormat1.format(date);
        SimpleDateFormat datetimeFormat2 = new SimpleDateFormat("HHmmss");
        String hhmmssStr = datetimeFormat2.format(date);
        String randStr = String.format("%04d",new Random().nextInt(9999)) ;
        String newFileNameNoExtension = "se-" + hhmmssStr+"-"+randStr ;//se for serial analyse

        String outdir = WConfig.getSharedInstance().pedir + "/offtask/" ;
        boolean dirok1 = FileDirTool.checkDirExistsOrCreate(outdir) ;
        if( dirok1==false ){
            result.setState(9);
            result.setMessage("bad  outdir.");
            return result ;
        }

        String outYmdDir = WConfig.getSharedInstance().pedir + "/offtask/"+yyyyMMddStr+"/" ;
        boolean dirok2 = FileDirTool.checkDirExistsOrCreate(outYmdDir) ;
        if( dirok2==false ){
            result.setState(9);
            result.setMessage("bad outYmdDir.");
            return result ;
        }

        String absOrderJsonFilepath = outYmdDir + newFileNameNoExtension + ".json" ;
        String absTempRoiGeoJsonFilepath = outYmdDir + newFileNameNoExtension + "-roi.geojson" ;
        String absTempRoiTlvFilepath = outYmdDir + newFileNameNoExtension + "-roi.hseg.tlv" ;
        String orderJsonRelFilepath = "offtask/" + yyyyMMddStr + "/" + newFileNameNoExtension + ".json" ;
        String resultRelFilepath = "offtask/" + yyyyMMddStr + "/" + newFileNameNoExtension + "-result.json" ;

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;

        String methodStr = "" ;
        if( cmethod.equals("1") ) methodStr = "min" ;
        else if( cmethod.equals("2")) methodStr="max";
        else if( cmethod.equals("3")) methodStr="ave" ;
        else if(cmethod.equals("4"))  methodStr = "sum" ;
        else{
            result.setState(9);
            result.setMessage("unsupported cmethod:" + cmethod);
            return result ;
        }

        JTileComputingSerialOrder orderObj = new JTileComputingSerialOrder() ;
        orderObj.dsname = dsname ;
        orderObj.method = methodStr ;
        orderObj.dt = 0L ;//not used yet
        orderObj.sdui = "" ;//not used yet
        orderObj.filldata = Double.valueOf(filldata);
        orderObj.jsfile="" ;//not supported yet
        orderObj.validMinInc = Double.valueOf(validmin);
        orderObj.validMaxInc = Double.valueOf(validmax) ;

        //构造ROI数据
        String roiStr = "" ;
        if( roiid.equals("") == false ){
            roiStr = roiid ;
        }else if( roigeojson.equals("") ==false ){
            //write geojson into file
            if( FileDirTool.writeToTextFile(absTempRoiGeoJsonFilepath,roigeojson) ==false ) {
                result.setState(9);
                result.setMessage("bad writing geojson.");
                return result ;
            }
            //convert geojson into tlv
            String tlvcmd = WConfig.getSharedInstance().shpgeojson2hsegtlv
                    + " " + absTempRoiGeoJsonFilepath + " "+ absTempRoiTlvFilepath;
            try{
                Runtime run  = Runtime.getRuntime();
                Process proc = run.exec(tlvcmd);
                proc.waitFor() ;
                File tlvfile = new File(absTempRoiTlvFilepath) ;
                if( tlvfile.exists()==false ){
                    result.setState(9);
                    result.setMessage("bad tlv file.");
                    return result ;
                }
                roiStr = absTempRoiTlvFilepath ;
            }catch (Exception ex1)
            {
                result.setState(9);
                result.setMessage("bad converting tlv.");
                return result ;
            }
        }else{
            result.setState(9);
            result.setMessage("no roi.");
            return result ;
        }

        orderObj.roi =roiStr ;
        orderObj.whole_start = Long.valueOf(wholestartdt);
        orderObj.whole_start_inc=1;
        orderObj.whole_stop = Long.valueOf(wholestopdt);
        orderObj.whole_stop_inc = 1 ;
        orderObj.repeat_type = repeattype ;
        orderObj.repeat_start = Long.valueOf(repeatstartdt);
        orderObj.repeat_start_inc = 1 ;
        orderObj.repeat_stop = Long.valueOf(repeatstopdt);
        orderObj.repeat_stop_inc = 1;
        orderObj.repeat_stopnextyear = Integer.valueOf(repeatstopnextyear);

        boolean writeOrderJsonOk = FileDirTool.writeToTextFile(absOrderJsonFilepath ,
                new Gson().toJson(orderObj,JTileComputingSerialOrder.class)) ;
        if( writeOrderJsonOk==false ){
            result.setState(9);
            result.setMessage("failed write order json.");
            return result ;
        }

        //写入离线任务到mysql
        int mode = 1 ;//0-zonalstat 1-skSerial 2-lsSerial 4-tcHbase 5-export
        int ofid =  rdb.rdbNewOffTask(  Integer.parseInt(uid) , mode , orderJsonRelFilepath,
                resultRelFilepath ) ;
        if( ofid>0 ){
            //这里添加zeromq调用 这里传递两个值 一个order主键和order json文件的相对路径
            JOfftaskOrderMsg msg=new JOfftaskOrderMsg() ;
            msg.ofid = ofid ;
            msg.mode = mode ;
            msg.orderRelFilepath = orderJsonRelFilepath ;
            JOfftaskOrderSender.getSharedInstance().send(msg);
            result.setState(0);
            result.setMessage("");
            result.setData("{\"ofid\":" + String.valueOf(ofid) + "}");
            return result ;
        }
        else{
            result.setState(9);
            result.setMessage("bad mysql offtask record.");
            return result ;
        }
    }

}
