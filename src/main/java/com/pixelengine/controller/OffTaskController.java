package com.pixelengine.controller;
//这个还没想好怎么改 2021-4-1 这个不再使用，请使用ZonalStatController，或者参考ZonalStatController修改。
//2022-4-5 使用该Controller 作为新版 离线任务 区域统计，序列分析，数据合成 接口
//2022-4-9 实现离线任务与数据的删除功能
//2022-9-9 GOTS

import com.google.gson.Gson;
import com.pixelengine.DataModel.JZonalStat2;
//import com.pixelengine.DTO.ZonalStatDTO;
import com.pixelengine.DataModel.*;
import com.pixelengine.HBasePixelEngineHelper;
import com.pixelengine.JRDBHelperForWebservice;
import com.pixelengine.tools.FileDirTool;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

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
        int newpdtid = rdb.rdbNewEmptyUserProduct(uuidDsname,Integer.valueOf(uid));
        if( newpdtid<=0 ){
            result.setState(9);
            result.setMessage("bad new product id.");
            return result ;
        }
        String newDsName="user/"+uid+"/"+String.valueOf(newpdtid);
        rdb.updateProductName(newpdtid,newDsName);

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
            boolean sendok = JOfftaskOrderSender.getSharedInstance().send(msg);
            if( sendok==false){
                rdb.updateOfftaskState(ofid,3);//failed.
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
            boolean sendok = JOfftaskOrderSender.getSharedInstance().send(msg);
            if( sendok==false){
                rdb.updateOfftaskState(ofid,3);//failed.
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
            boolean sendok = JOfftaskOrderSender.getSharedInstance().send(msg);
            if( sendok==false){
                rdb.updateOfftaskState(ofid,3);//failed.
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
            result.setMessage("bad mysql offtask record.");
            return result ;
        }
    }


    //离线任务列表
    @CrossOrigin(origins = "*")
    @GetMapping("/list")
    @ResponseBody
    public RestResult getlist(
            String uid,
            String ipage,  //base 0
            String pagesize
    ){
        RestResult result = new RestResult() ;

        int iuid = Integer.valueOf(uid) ;
        int iiPage = Integer.valueOf(ipage) ;
        int iPagesize = Integer.valueOf(pagesize) ;

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
        int allcount = rdb.rdbGetOfftaskCountByUid( iuid ) ;

        Dictionary<String,Object> map = new Hashtable<>() ;
        map.put("allcount" , allcount) ;
        if( allcount>0 ){
            ArrayList<JOfftask> tlist = rdb.rdbGetOfftaskList(iuid ,iiPage , iPagesize)  ;
            if( tlist == null ){
                result.setState(9);
                result.setMessage("failed to get offtask list.");
                return result ;
            }
            map.put("list" , tlist.toArray(new JOfftask[0]) ) ;
        }else{
            map.put("list" , new JOfftask[0] ) ;
        }
        result.setState(0);
        result.setData(map);
        return result ;
    }


    ///删除离线任务，及其对应的数据文件和数据库记录包括HBase和Mysql中的 2022-4-9
    @CrossOrigin(origins = "*")
    @PostMapping("/remove")
    @ResponseBody
    public RestResult remove(
            String ofid
    ){
        RestResult result = new RestResult() ;
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
        JOfftask offtask1 = rdb.rdbGetOfftask( Integer.valueOf(ofid) ) ;
        if( offtask1==null ){
            result.setState(9);
            result.setMessage("no recored for " + ofid);
            return result ;
        }

        String orderfile = WConfig.getSharedInstance().pedir + offtask1.orderfile ;
        String resultfile = WConfig.getSharedInstance().pedir + offtask1.resultfile ;

        boolean clean = false ;
        if( offtask1.mode== 0 ){
            File file1 = new File(orderfile) ;
            file1.delete() ;
            File file2 = new File(resultfile) ;
            file2.delete() ;
            File file3 = new File(orderfile.replace(".json","-roi.geojson")) ;
            file3.delete() ;
            File file4 = new File(orderfile.replace(".json","-roi.hseg.tlv")) ;
            file4.delete() ;

            clean = true ;
        }else if( offtask1.mode==1 || offtask1.mode==2 )
        {
            File file1 = new File(orderfile) ;
            file1.delete() ;
            File file2 = new File(resultfile) ;
            file2.delete() ;
            File file3 = new File(orderfile.replace(".json","-roi.geojson")) ;
            file3.delete() ;
            File file4 = new File(orderfile.replace(".json","-roi.hseg.tlv")) ;
            file4.delete() ;
            File file5 = new File(resultfile.replace(".json",".json.csv")) ;
            file5.delete() ;
            clean = true ;
        }else if( offtask1.mode==5 ){
            File file1 = new File(orderfile) ;
            file1.delete() ;
            File file2 = new File(resultfile) ;
            file2.delete() ;
            File file3 = new File(orderfile.replace(".json",".geojson")) ;
            file3.delete() ;



            File file6 = new File(orderfile.replace(".json","_pe.js")) ;
            file6.delete() ;
            File file7 = new File(resultfile.replace("-result.json","-result_tiled.tif")) ;
            file7.delete() ;
            File file8 = new File(resultfile.replace("-result.json","-result.tif")) ;
            file8.delete() ;

            clean = true ;
        }else if( offtask1.mode==4 ){
            int mypid = 0 ;
            try{
                JTileComputing2HBaseOrder tc2hbOrder = null;
                String orderjsontext = FileDirTool.readFileAsString(orderfile) ;
                if( orderjsontext!=null ){
                    tc2hbOrder = new Gson()
                            .fromJson(orderjsontext,JTileComputing2HBaseOrder.class);
                    mypid = tc2hbOrder.mpid_hpid ;
                }
                if( mypid<=0 ){
                    //there is no order json file , so just go following codes without hbase staff.
                }
                else
                {
                    //delete data in hbase
                    JProduct temppdtinfo = rdb.rdbGetProductForAPI(mypid) ;
                    if( temppdtinfo!=null )
                    {
                        Connection conn = HBasePixelEngineHelper.getHBaseConnection();
                        Table table = conn.getTable(TableName.valueOf(tc2hbOrder.out_htable));
                        ArrayList<Delete> dellist = new ArrayList<>() ;
                        for(int iz = 0 ; iz <= temppdtinfo.maxZoom; ++ iz )
                        {
                            int tilexnum = (int)Math.pow(2,iz) ;
                            int tileynum = tilexnum/2 ;
                            for(int iy = 0 ; iy < tileynum; ++ iy )
                            {
                                for(int ix = 0 ; ix < tilexnum; ++ ix )
                                {
                                    byte[] rowkey = WHBaseUtil.GenerateRowkey( tc2hbOrder.out_hpidlen,
                                            mypid ,
                                            tc2hbOrder.out_xylen ,
                                            iz, iy , ix ) ;
                                    dellist.add(new Delete(rowkey)) ;
                                    if( dellist.size()> 1000 ){
                                        table.delete(dellist);
                                        dellist.clear();
                                    }
                                }
                            }
                        }
                        if( dellist.size()>0 ) table.delete(dellist);
                        table.close();
                        System.out.println("delete data in hbase ok for mypid:" + mypid )  ;
                    }else{
                        System.out.println("warning tc2hb mypid "+mypid+" order ok, " +
                                "but no product info. that's ok, we clean other staffs.");
                    }
                }

            }

            catch(Exception ex)
            {
                result.setState(9);
                result.setMessage("Error : failed to parse order json for mypid:" + orderfile );
                return result ;
            }

            File file1 = new File(orderfile) ;
            file1.delete() ;
            File file2 = new File(resultfile) ;
            file2.delete() ;

            File file3 = new File(orderfile.replace(".json","-roi.geojson")) ;
            file3.delete() ;
            File file4 = new File(orderfile.replace(".json","-roi.hseg.tlv")) ;
            file4.delete() ;

            File file6 = new File(orderfile.replace(".json",".js")) ;
            file6.delete() ;

            if( mypid>0 ){
                boolean myok = rdb.rdbRemoveProductRecords(mypid) ;
                if( myok==false )
                {
                    result.setState(9);
                    result.setMessage("Error : failed to mysql records for mypid:" + mypid );
                    return result ;
                }
            }

            clean = true ;
        }
        else if( offtask1.mode==6 )
        {
            File file1 = new File(orderfile) ;
            file1.delete() ;
            File file2 = new File(resultfile) ;
            file2.delete() ;
            clean = true ;
        }
        else{
            result.setState(9);
            result.setMessage("unknown offtask mode for " + offtask1.mode );
            return result ;
        }

        if( clean==false ){
            result.setState(9);
            result.setMessage("remove operation is not clean.");
            return result ;
        }

        //delete offtask records
        rdb.rdbRemoveOfftaskRecords( Integer.valueOf(ofid)) ;
        result.setState(0);
        result.setData(ofid);

        return result ;
    }


    public static String getScriptTextBySid(int sid){
        JRDBHelperForWebservice rdb =new JRDBHelperForWebservice();
        JScript sc = rdb.rdbGetScript( sid ) ;
        if( sc==null ){
            return null ;
        }else
        {
            return ScriptsController.sgetScriptContent( sc.jsfile) ;
        }
    }


    public static String insertSduiObject(String scriptText0,String sduiObj)
    {
        if( sduiObj==null || sduiObj.equals("") || sduiObj.equals("null") || sduiObj.equals("{}") )
        {
            return scriptText0 ;
        }
        String[] sduiDeclArr = new String[]{
                "let sdui =",
                "let sdui=",
                "var sdui =",
                "var sdui="
        };
        for(int i = 0 ; i<sduiDeclArr.length;++i )
        {
            int index0 = scriptText0.indexOf( sduiDeclArr[i] ) ;
            if( index0>=0 )
            {
                int index1 = scriptText0.indexOf(";", index0+1) ;
                if( index1>=0 )
                {
                    String part0 = scriptText0.substring(0, index1+1) ;
                    String part1 = scriptText0.substring(index1+1  ) ;
                    String scriptText1 = part0 + "\nsdui="+ sduiObj + ";" + part1 ;
                    return scriptText1 ;
                }
            }
        }
        return scriptText0 ;
    }


    //新建GOTS
    @CrossOrigin(origins = "*")
    @PostMapping("/gots/new")
    @ResponseBody
    public RestResult gotsNew(
            String uid,
            String gotssid,
            String sdui,
            String dt
    ) {
        System.out.println("gotsNew");
        RestResult result = new RestResult();

        //write into file
        Date date = new Date();
        SimpleDateFormat datetimeFormat1 = new SimpleDateFormat("yyyyMMdd");
        String yyyyMMddStr = datetimeFormat1.format(date);
        SimpleDateFormat datetimeFormat2 = new SimpleDateFormat("HHmmss");
        String hhmmssStr = datetimeFormat2.format(date);
        String randStr = String.format("%04d",new Random().nextInt(9999)) ;
        String newFileNameNoExtension = "gots-" + hhmmssStr+"-"+randStr ;//se for serial analyse

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
        //pe.extraData={};
        //pe.extraData.datetime=dt;
        //pe.extraData.jsfile=absOrderJsFilepath;
        //pe.extraData.resultfile=absResultFilepath;
        //pe.extraData.pedir=task17config.pedir
        //pe.extraData.http_pedir=task17config.http_pedir
        String absOrderJsFilepath = outYmdDir + newFileNameNoExtension + ".js" ;
        String absResultFilepath = outYmdDir + newFileNameNoExtension + "-result.json" ;
        String orderJsRelFilepath = "offtask/" + yyyyMMddStr + "/" + newFileNameNoExtension + ".js" ;
        String resultRelFilepath = "offtask/" + yyyyMMddStr + "/" + newFileNameNoExtension + "-result.json" ;
        String peExtraDataCode="pe.extraData={};\n" ;
        peExtraDataCode+="pe.extraData.datetime="+dt+";\n" ;
        peExtraDataCode+="pe.extraData.jsfile='"+absOrderJsFilepath+"';\n" ;
        peExtraDataCode+="pe.extraData.resultfile='"+absResultFilepath+"';\n" ;
        peExtraDataCode+="pe.extraData.pedir='"+WConfig.getSharedInstance().pedir+"';\n" ;
        peExtraDataCode+="pe.extraData.http_pedir='"+WConfig.getSharedInstance().http_pedir+"';\n" ;

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;

        String scriptText0 = OffTaskController.getScriptTextBySid(Integer.parseInt(gotssid)) ;
        if( scriptText0==null ){
            result.setState(10);
            result.setMessage("null script text.");
            return result ;
        }
        if( scriptText0.equals("") ){
            result.setState(11);
            result.setMessage("empty script text.");
            return result ;
        }

        //insert extraData and  sdui object
        String scriptText1 =
                peExtraDataCode+
                OffTaskController.insertSduiObject(scriptText0 , sdui) ;

        if( FileDirTool.writeToTextFile(absOrderJsFilepath,scriptText1) ==false ) {
            result.setState(12);
            result.setMessage("bad writing jsfile.");
            return result ;
        }

        //写入离线任务到mysql
        int mode = 6 ;//0-zonalstat 1-skSerial 2-lsSerial 4-tcHbase 5-export 6-gots
        int ofid =  rdb.rdbNewOffTask(  Integer.parseInt(uid) , mode , orderJsRelFilepath,
                resultRelFilepath ) ;
        if( ofid>0 ){
            //这里添加zeromq调用 这里传递两个值 一个order主键和order json文件的相对路径
            JOfftaskOrderMsg msg=new JOfftaskOrderMsg() ;
            msg.ofid = ofid ;
            msg.mode = mode ;
            msg.orderRelFilepath = orderJsRelFilepath ;
            boolean sendok = JOfftaskOrderSender.getSharedInstance().send(msg);
            if( sendok==false){
                rdb.updateOfftaskState(ofid,3);//failed.
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
            result.setState(14);
            result.setMessage("mysql failed to insert.");
            return result ;
        }
    }

}
