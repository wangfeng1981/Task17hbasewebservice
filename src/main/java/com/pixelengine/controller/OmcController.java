package com.pixelengine.controller;
/// 在线作图java这边相关接口，主要是mysql相关操作 2022-4-17
/// 2022-4-17 created
/// 2022-4-25
// 2022-5-25
//2022-7-26

import com.google.gson.Gson;
import com.pixelengine.DataModel.*;
import com.pixelengine.HBasePeHelperCppConnector;
import com.pixelengine.HBasePixelEngineHelper;
import com.pixelengine.JRDBHelperForWebservice;
import com.pixelengine.tools.FileDirTool;
import com.pixelengine.tools.HttpTool;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.tomcat.jni.FileInfo;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.jcodings.util.Hash;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
//import scala.reflect.internal.tpe.FindMembers;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

@RestController
@RequestMapping("/omc")
public class OmcController {


    public void TestPost () {
        String omcApi = WConfig.getSharedInstance().omc_localhost_api ;
        //新新建一个qgs项目
        String qgsfile = "" ;
        {
            HttpTool http = new HttpTool() ;
            if( http.omcRpc(omcApi ,  "project.new" , "{}") ==0 )
            {
                System.out.println("Result:");
                System.out.println(http.getResult());
            }else{
                System.out.println("Error:\n" + http.getError());
            }
        }
    }


    //y用户文件列表
    @CrossOrigin(origins = "*")
    @RequestMapping("/filelist")
    @ResponseBody
    public RestResult getFileList(String uid,
                                  String type  // 1 qgs| 2 img | 3 vec
    ) {

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
        ArrayList<OmcFile> data = rdb.rdbGetOmcFileList( Integer.valueOf(uid) , Integer.valueOf(type) ) ;
        if( data==null ){
            RestResult result = new RestResult();
            result.setState(1);
            result.setMessage("rdbGetOmcFileList exception.");
            result.setData("");
            return result ;
        }else{
            RestResult result = new RestResult();
            result.setState(0);
            result.setMessage("");
            result.setData(data);
            return result ;
        }

    }


    //create a new qgs project and insert into db.
    @CrossOrigin(origins = "*")
    @RequestMapping("/newqgs")
    @ResponseBody
    public RestResult newQgs(
            String uid,
            String pid,
            String sid,
            String datetime,
            String sdui,
            String roiid,
            String styleid
    )
    {
        RestResult rr = new RestResult() ;
        String capurl = "" ;
        String wmsLayer = "" ;

        String omcApi = WConfig.getSharedInstance().omc_localhost_api ;

        //新新建一个qgs项目
        String relQgsfile = "" ;
        {
            HttpTool http1 = new HttpTool() ;
            int ret1 = http1.omcRpc(omcApi , "project.new" , "{}" ) ;
            if( ret1!=0 ){
                rr.setState(11);
                rr.setMessage(http1.getError());
                return rr ;
            }
            relQgsfile = ((Map)http1.getResult().get("data")).get("file").toString() ;
        }

        int ipid = Integer.parseInt(pid) ;
        int isid = Integer.parseInt(sid) ;

        WConfig c = WConfig.getSharedInstance() ;
        int maxZoom = 0 ;

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
        if( ipid > 0 ){
            //http://192.168.56.103:15900/pe/product/1/wmts/WMTSCapabilities.xml
            //layer layer_{pid}
            capurl = WConfig.getSharedInstance().task17_api_root+ "product/"+pid+"/wmts/WMTSCapabilities.xml" ;
            wmsLayer = "layer_"+pid ;

            JProduct pdt1 = rdb.rdbGetProductForAPI(ipid) ;
            if( pdt1==null ){
                rr.setState(2); rr.setMessage("can not find product info by "+pid);
                return rr ;
            }
            maxZoom = pdt1.maxZoom ;

        }else if( isid > 0 ){
            //http://192.168.56.103:15900/pe/scripts/1/wmts/WMTSCapabilities.xml
            //layer script_{sid}
            capurl = WConfig.getSharedInstance().task17_api_root+"scripts/"+sid+"/wmts/WMTSCapabilities.xml" ;
            wmsLayer = "script_"+sid ;

            JScript script1 = rdb.rdbGetScript(isid) ;
            if(script1==null) {
                rr.setState(2); rr.setMessage("no script in db for sid "+sid);
                return rr ;
            }
            String absJsFile = c.pedir + script1.jsfile ;
            try{
                String scriptText = FileDirTool.readFileAsString(absJsFile) ;
                HBasePeHelperCppConnector cc = new HBasePeHelperCppConnector() ;
                String jsontext = cc.GetDatasetNameArray("com/pixelengine/HBasePixelEngineHelper" ,scriptText ) ;
                Gson gson = new Gson() ;
                JDsNameArrayResult dsArrResult = gson.fromJson( jsontext , JDsNameArrayResult.class ) ;
                if( dsArrResult.data.length  == 0 ){
                    throw new Exception("can not parse dataset name from script text("+sid+").") ;
                }else{
                    JProduct pdt2 = rdb.rdbGetProductInfoByName(dsArrResult.data[0]) ;
                    if( pdt2==null ) {
                        rr.setState(2); rr.setMessage("can not find product info by dsname:'"+dsArrResult.data[0]+"'.");
                        return rr ;
                    }
                    maxZoom = pdt2.maxZoom ;
                }
            }catch (Exception ex){
                rr.setState(2); rr.setMessage(ex.getMessage());
                return rr ;
            }
        }else{
            rr.setState(1);
            rr.setMessage("pid and sid both invalid.");
            return rr ;
        }

        String tms = "ms_" + String.valueOf(maxZoom) ;
        Map<String,String> map2 = new HashMap<>() ;
        map2.put("file" , relQgsfile) ;
        map2.put("capurl" , capurl) ;
        map2.put("tms" , tms) ;
        map2.put("layers", wmsLayer) ;
        map2.put("styleid" , styleid) ;
        map2.put("datetime" , datetime ) ;
        map2.put("sdui" , sdui) ;
        map2.put("roiid" , roiid) ;

        {
            Gson gson2 = new Gson () ;
            String jsondata2 = gson2.toJson(map2,Map.class) ;
            HttpTool http2 = new HttpTool() ;
            int ret2 = http2.omcRpc(omcApi , "project.addwms" , jsondata2) ;
            if( ret2==0 ){
                //写入数据库
                String currdt = rdb.getCurrentDatetimeStr() ;
                int ret3 = rdb.insertNewOmcFile(1,0,relQgsfile, Integer.valueOf(uid) , currdt ) ;
                if( ret3>=0 ){
                    rr.setState(0);
                    rr.setData( relQgsfile );
                    return rr ;
                }else{
                    rr.setState(30);
                    rr.setMessage("qgsfile failed to insert into db.");
                    return rr ;
                }

            }else{
                rr.setState(ret2);
                rr.setMessage( http2.getError());
                return rr ;
            }
        }
//        {
//            "file":"omc_out/20220417/122550-4499.qgs",
//                "capurl":"http://192.168.56.103:15900/pe/product/1/wmts/WMTSCapabilities.xml",
//                "tms":"ms_5",
//                "layers":"layer_1",
//                "styleid":2,
//                "datetime":20200600000000,
//                "sdui":"null",
//                "roiid":"sys:1"
//        }
    }


    //将文件写入上传文件对象file写入系统指定路径fileAbsPath
    public boolean writeUploadFileToFile(MultipartFile file,String fileAbsPath ){
        String fileName = file.getOriginalFilename();
        File targetFile = new File(fileAbsPath);
        //第三部：通过输出流将文件写入硬盘文件夹并关闭流
        BufferedOutputStream stream = null;
        boolean isok = false ;
        try {
            stream = new BufferedOutputStream(new FileOutputStream(fileAbsPath));
            stream.write(file.getBytes());
            stream.flush();
            stream.close();
            stream = null ;
            isok=true ;
        }catch (IOException e){
            e.printStackTrace();
            isok=false ;
        }finally {
            try {
                if (stream != null) stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return isok ;
    }

    //upload image , should be < 1MB, only png or jpg.
    //upload path in omc_out/{yyyymmdd}/{hhmmss}-{rrrr}.png|jpg
    @CrossOrigin(origins = "*")
    @RequestMapping("/uploadimg")
    @ResponseBody
    public RestResult uploadImg (
            String uid,
            MultipartFile[] files, //only one file once.
            HttpServletRequest httpServletRequest
    ){
        System.out.println("OmcController uploadImg ...");
        RestResult rr = new RestResult() ;
        rr.setState(1);
        rr.setMessage("");

        if( files.length==0 ){
            rr.setState(2); rr.setMessage("empty files.");
            return rr ;
        }

        String tempfilename = files[0].getOriginalFilename() ;
        String tailname = "" ;
        if( tempfilename.endsWith(".png") ) tailname = ".png" ;
        if( tempfilename.endsWith(".jpg") ) tailname = ".jpg" ;
        if( tailname.equals("") ){
            rr.setState(3); rr.setMessage("only support .png or .jpg image file.");
            return rr ;
        }

        FileDirTool.FileNamerResult fileNameResult = FileDirTool.buildDatetimeSubdirAndFilename(
                WConfig.getSharedInstance().pedir , "omc_out" , "img-", tailname
        ) ;

        if( fileNameResult.state!=0 ){
            rr.setState(2);
            rr.setMessage(fileNameResult.message);
            return rr ;
        }

        //copy file into new file position
        boolean cpok = writeUploadFileToFile(files[0], fileNameResult.data.absfilename) ;
        if( cpok==false ){
            rr.setState(4);
            rr.setMessage("failed to copy upload image file.");
            return rr ;
        }

        if( tempfilename.length()>20 ) tempfilename = tempfilename.substring( tempfilename.length()-20 ) ;

        //insert into db
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
        int newRid = rdb.insertNewOmcFile(2 , 0 ,
                fileNameResult.data.relfilename, Integer.valueOf(uid) ,
                tempfilename
                ) ;
        if( newRid < 0 ){
            rr.setState(5); rr.setData("failed to insert db.");
            return rr ;
        }
        rr.setState(0); rr.setData(fileNameResult.data.relfilename);
        return rr ;
    }


    // this shp only used in omc. this shp will not convert geojson, since qgis can draw shp.
    @CrossOrigin(origins = "*")
    @RequestMapping("/uploadshp")
    @ResponseBody
    public RestResult uploadShp (
            String uid,
            MultipartFile[] files, //
            HttpServletRequest httpServletRequest
    ){
        RestResult rr = new RestResult() ;

        FileDirTool.FileNamerResult fileNamerResult = FileDirTool.buildDatetimeSubdirAndFilename(
                WConfig.getSharedInstance().pedir,
                "omc_out",
                "vec-" ,
                ""
        ) ;

        if( fileNamerResult.state!=0 ){
            rr.setState(1);
            rr.setMessage(fileNamerResult.message);
            return rr ;
        }

        if( files.length < 4 ){
            rr.setState(2);
            rr.setMessage("Not enough four files.");
            return rr ;
        }


        //查询是否够四个文件，够了入库
        boolean shpBoo = false;
        boolean dbfBoo = false;
        boolean prjBoo = false;
        boolean shxBoo = false;
        String nameInDb = "" ;
        for(int ifile = 0 ; ifile < files.length;++ ifile ){
            String tempfilename = files[ifile].getOriginalFilename() ;
            if(tempfilename.endsWith(".shp")){
                String newfilepath = fileNamerResult.data.absfilename +".shp" ;
                shpBoo = writeUploadFileToFile( files[ifile] , newfilepath );
                nameInDb = files[ifile].getOriginalFilename() ;
            }
            if(tempfilename.endsWith(".dbf")){
                String newfilepath = fileNamerResult.data.absfilename +".dbf" ;
                dbfBoo = writeUploadFileToFile( files[ifile] , newfilepath );
            }
            if(tempfilename.endsWith(".prj")){
                String newfilepath = fileNamerResult.data.absfilename +".prj" ;
                prjBoo = writeUploadFileToFile( files[ifile] , newfilepath );
            }
            if(tempfilename.endsWith(".shx")){
                String newfilepath = fileNamerResult.data.absfilename +".shx" ;
                shxBoo = writeUploadFileToFile( files[ifile] , newfilepath );
            }
        }

        if( !shpBoo || !shxBoo || !prjBoo || !dbfBoo )
        {
            rr.setState(3);
            rr.setMessage("failed to write shp "+shpBoo + ", shx "+shxBoo + ", dbf "+dbfBoo + ", prj "+prjBoo);
            return rr ;
        }

        String shpRelPath = fileNamerResult.data.relfilename+".shp";
        String shpAbsPath = fileNamerResult.data.absfilename+".shp";

        //shape file type
        int iGeomType = 0 ;
        try{
            ShapefileDataStore dataStore =
                    new ShapefileDataStore(
                            new File(shpAbsPath).toURI().toURL()
                    );
            String t = dataStore.getTypeNames()[0];
            String geomType = dataStore.getFeatureSource(t)
                    .getSchema().getGeometryDescriptor()
                    .getType().getBinding().getName();
            System.out.println(geomType);
            if( geomType.contains("Point")  ){
                iGeomType = 1 ;
            }else if( geomType.contains("Polygon")  ){
                iGeomType = 3 ;
            }else if( geomType.contains("Line")   ){
                iGeomType = 2 ;
            }else{
                rr.setState(4);
                rr.setMessage("not supported GeomType:" + geomType);
                return rr ;
            }
        }catch(Exception ex){
            rr.setState(5);
            rr.setMessage(ex.getMessage());
            return rr ;
        }


        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
        int newRid = rdb.insertNewOmcFile(3 , iGeomType , shpRelPath , Integer.valueOf(uid) ,nameInDb );
        if( newRid < 0 ){
            rr.setState(5);
            rr.setMessage("failed to insert db.");
            return rr ;
        }

        rr.setState(0);
        rr.setMessage("");
        rr.setData(shpRelPath);
        return rr ;
    }


    // delete some file
    @CrossOrigin(origins = "*")
    @RequestMapping("/delfile")
    @ResponseBody
    public RestResult delFile (
            String omcid
    ){
        RestResult rr = new RestResult() ;

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
        OmcFile ofile = rdb.rdbGetOmcFile( Integer.valueOf(omcid)) ;
        if( ofile==null ){
            rr.setState(1);
            rr.setMessage("can not find omc file.");
            rr.setData("");
            return rr ;
        }

        int lastSlashIndex = ofile.file.lastIndexOf('/') ;
        int lastDotIndex = ofile.file.lastIndexOf('.') ;
        String matchingFileName ="" ;
        if( lastDotIndex>=0 && lastSlashIndex>=0 && lastSlashIndex < lastDotIndex )
        {
            //ok
            matchingFileName = ofile.file.substring(lastSlashIndex+1, lastDotIndex-1 ) ;
            System.out.println("matchingFileName for delete:"+matchingFileName);
        }
        if( matchingFileName.equals("") ){
            rr.setState(2);
            rr.setMessage("can not build matching filename for deleting.");
            rr.setData("");
            return rr ;
        }

        String theLastDirPath =
                WConfig.getSharedInstance().pedir+ofile.file.substring(0,lastSlashIndex);
        int numdel = 0 ;
        try{
            File dirPath = new File(theLastDirPath);
            File filesList[] = dirPath.listFiles();
            for(File file : filesList) {
                if(file.isFile()) {
                    if( file.getName().contains(matchingFileName) ){
                        ++numdel ;
                        file.delete();
                    }
                }
            }
        }catch (Exception ex){
            rr.setState(3);
            rr.setMessage(ex.getMessage());
            rr.setData("");
            return rr ;
        }

        rdb.deleteOmcFile( Integer.valueOf(omcid) ) ;

        rr.setState(0);
        rr.setMessage("");
        rr.setData("delcnt:"+numdel);
        return rr ;
    }


    // make thumb png file for rel qgs file 2022-5-26
    private boolean makeQgsThumbFile( String relqgsfile )
    {
        String thumbfile = relqgsfile + ".thumb.png" ;
        HashMap<String,String> map1 = new HashMap<>();
        map1.put("file",relqgsfile);
        map1.put("outfile",thumbfile) ;
        Gson gson = new Gson();
        String jsondata = gson.toJson( map1 , HashMap.class);
        String omcApi = WConfig.getSharedInstance().omc_localhost_api ;
        HttpTool http2 = new HttpTool() ;
        int ret2 = http2.omcRpc(omcApi , "layout.makethumb" , jsondata) ;
        if( ret2==0 ){
            return true ;
        }else{
            return false;
        }
    }



    // 模板文件列表
    // [ {catid:1,catname:'', data:[...]}, {catid:2,catname:'', data:[...]} ,...]
    @CrossOrigin(origins = "*")
    @RequestMapping("/temlist")
    @ResponseBody
    public RestResult getTemList(String uid ) {
        RestResult rr = new RestResult();
        rr.setState(0);
        rr.setMessage("");

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
        ArrayList<JGeneralCategory> catlist = rdb.rdbGetGeneralCategoryList(3,1) ;
        if( catlist==null ){
            rr.setState(1);
            rr.setMessage("get category list failed.");
            return rr ;
        }

        for(int i = 0 ; i<catlist.size();++i )
        {
            int catid1 = catlist.get(i).catid ;
            ArrayList<OmcFile> filelist = rdb.rdbGetOmcFileListWithType2(0 , 4 , catid1 ) ;
            catlist.get(i).data = filelist.toArray(new Object[0]) ;
            //检查是否有同名xxx.qgs.thumb.png缩略图文件，如果没有生成一个
            for(int j = 0 ; j<catlist.get(i).data.length;++ j )
            {
                OmcFile omcfile1 = (OmcFile)catlist.get(i).data[j] ;
                String qgsfile = omcfile1.file ;
                String thumbfile = qgsfile + ".thumb.png" ;
                String absThumbfile = WConfig.getSharedInstance().pedir + thumbfile ;
                File file1 = new File( absThumbfile ) ;
                if( file1.exists() ==false ){
                    //make a .thumb.png
                    boolean thumbok = makeQgsThumbFile(qgsfile) ;
                    if( thumbok==true ){
                        System.out.println("make thumb ok for " + qgsfile);
                    }
                }
            }
        }
        rr.setState(0);
        rr.setData(catlist);
        return rr ;

    }



    //create a new qgs project from template.
    @CrossOrigin(origins = "*")
    @RequestMapping("/newfromtem")
    @ResponseBody
    public RestResult newFromTemplate(
            String uid,
            String pid,
            String sid,
            String datetime,
            String sdui,
            String roiid,
            String styleid,
            String temfile,      // relfilepath_of_template
            String isautozoom,   //0-no zoom, 1-zoom, this field only for use template
            String left,  //for isAutoZoom=1
            String right, //for isAutoZoom=1
            String top,   //for isAutoZoom=1
            String bottom //for isAutoZoom=1
    )
    {
        RestResult rr = new RestResult() ;
        String capurl = "" ;
        String wmsLayer = "" ;

        if( temfile==null || temfile.isEmpty() ){
            rr.setState(1);
            rr.setMessage("empty temfile.");
            return rr ;
        }

        String omcApi = WConfig.getSharedInstance().omc_localhost_api ;

        //新新建一个qgs项目
        String relQgsfile = "" ;
        {
            HttpTool http1 = new HttpTool() ;
            int ret1 = http1.omcRpc(omcApi , "project.newfromtem" , "{\"temfile\":\""+temfile+"\"}" ) ;
            if( ret1!=0 ){
                rr.setState(11);
                rr.setMessage(http1.getError());
                return rr ;
            }
            relQgsfile = ((Map)http1.getResult().get("data")).get("file").toString() ;
        }

        //isautozoom==1 自动缩放到经纬度范围
        System.out.println("isautozoom:" + isautozoom) ;
        if( isautozoom.equals("1") ){
            HttpTool http1 = new HttpTool() ;
            int ret1 = http1.omcRpc(omcApi , "project.zoom" ,
                    "{\"file\":\"" + relQgsfile + "\","
                            + "\"left\":"+left+","
                            + "\"right\":" + right+","
                            + "\"top\":" + top+","
                            + "\"bottom\":" + bottom
                            + "}" ) ;
            if( ret1!=0 ){
                rr.setState(11);
                rr.setMessage(http1.getError());
                return rr ;
            }
        }

        int ipid = Integer.parseInt(pid) ;
        int isid = Integer.parseInt(sid) ;
        WConfig c = WConfig.getSharedInstance() ;
        int maxZoom = 0 ;
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
        if( ipid > 0 ){
            //http://192.168.56.103:15900/pe/product/1/wmts/WMTSCapabilities.xml
            //layer layer_{pid}
            capurl = WConfig.getSharedInstance().task17_api_root+ "product/"+pid+"/wmts/WMTSCapabilities.xml" ;
            wmsLayer = "layer_"+pid ;

            JProduct pdt1 = rdb.rdbGetProductForAPI(ipid) ;
            if( pdt1==null ){
                rr.setState(2); rr.setMessage("can not find product info by "+pid);
                return rr ;
            }
            maxZoom = pdt1.maxZoom ;

        }else if( isid > 0 ){
            //http://192.168.56.103:15900/pe/scripts/1/wmts/WMTSCapabilities.xml
            //layer script_{sid}
            capurl = WConfig.getSharedInstance().task17_api_root+"scripts/"+sid+"/wmts/WMTSCapabilities.xml" ;
            wmsLayer = "script_"+sid ;

            JScript script1 = rdb.rdbGetScript(isid) ;
            if(script1==null) {
                rr.setState(2); rr.setMessage("no script in db for sid "+sid);
                return rr ;
            }
            String absJsFile = c.pedir + script1.jsfile ;
            try{
                String scriptText = FileDirTool.readFileAsString(absJsFile) ;
                HBasePeHelperCppConnector cc = new HBasePeHelperCppConnector() ;
                String jsontext = cc.GetDatasetNameArray("com/pixelengine/HBasePixelEngineHelper" ,scriptText ) ;
                Gson gson = new Gson() ;
                JDsNameArrayResult dsArrResult = gson.fromJson( jsontext , JDsNameArrayResult.class ) ;
                if( dsArrResult.data.length  == 0 ){
                    throw new Exception("can not parse dataset name from script text(sid:"+sid+").") ;
                }else{
                    JProduct pdt2 = rdb.rdbGetProductInfoByName(dsArrResult.data[0]) ;
                    if( pdt2==null ) {
                        rr.setState(2); rr.setMessage("can not find product info by dsname:'"+dsArrResult.data[0]+"'.");
                        return rr ;
                    }
                    maxZoom = pdt2.maxZoom ;
                }
            }catch (Exception ex){
                rr.setState(2); rr.setMessage(ex.getMessage());
                return rr ;
            }
        }else{
            rr.setState(1);
            rr.setMessage("pid and sid both invalid.");
            return rr ;
        }

        String tms = "ms_" + String.valueOf(maxZoom) ;
        Map<String,String> map2 = new HashMap<>() ;
        map2.put("file" , relQgsfile) ;
        map2.put("capurl" , capurl) ;
        map2.put("tms" , tms) ;
        map2.put("layers", wmsLayer) ;
        map2.put("styleid" , styleid) ;
        map2.put("datetime" , datetime ) ;
        map2.put("sdui" , sdui) ;
        map2.put("roiid" , roiid) ;

        {
            Gson gson2 = new Gson () ;
            String jsondata2 = gson2.toJson(map2,Map.class) ;
            HttpTool http2 = new HttpTool() ;
            int ret2 = http2.omcRpc(omcApi , "project.addwms" , jsondata2) ;
            if( ret2==0 ){
                //写入数据库
                String currdt = rdb.getCurrentDatetimeStr() ;
                int ret3 = rdb.insertNewOmcFile(1,0,relQgsfile, Integer.valueOf(uid) , currdt ) ;
                if( ret3>=0 ){
                    rr.setState(0);
                    rr.setData( relQgsfile );
                    return rr ;
                }else{
                    rr.setState(30);
                    rr.setMessage("qgsfile failed to insert into db.");
                    return rr ;
                }

            }else{
                rr.setState(ret2);
                rr.setMessage( http2.getError());
                return rr ;
            }
        }
//        {
//            "file":"omc_out/20220417/122550-4499.qgs",
//                "capurl":"http://192.168.56.103:15900/pe/product/1/wmts/WMTSCapabilities.xml",
//                "tms":"ms_5",
//                "layers":"layer_1",
//                "styleid":2,
//                "datetime":20200600000000,
//                "sdui":"null",
//                "roiid":"sys:1"
//        }
    }

}
