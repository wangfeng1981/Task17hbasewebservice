package com.pixelengine.controller;
/// 新版ROI接口，用于取代RegionController和AreaController 2022-2-2

import com.pixelengine.DataModel.JRoi2;
import com.pixelengine.DataModel.JRoiCategory;
import com.pixelengine.DataModel.RestResult;
import com.pixelengine.HBasePeHelperCppConnector;
import com.pixelengine.HBasePixelEngineHelper;
import com.pixelengine.JRDBHelperForWebservice;
import com.pixelengine.DataModel.WConfig;
import com.pixelengine.tools.FileDirTool;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/roi")
public class RoiController {

    //获取系统ROI分类
    @CrossOrigin(origins = "*")
    @RequestMapping("/cat")
    @ResponseBody
    public RestResult getCats() {
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
        ArrayList<JRoiCategory> data = rdb.rdbGetSysRoiCategories() ;
        for(int i =0 ; i<data.size();++i ){
            data.get(i).count = rdb.rdbGetSysRoiItemesCount(data.get(i).rcid ) ;
        }
        RestResult result = new RestResult();
        result.setState(0);
        result.setMessage("");
        result.setData(data);
        return result ;
    }

    //获取系统ROI分类item array
    @CrossOrigin(origins = "*")
    @RequestMapping("/sys")
    @ResponseBody
    public RestResult getSystemRois(int cat, int offset) {
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
        ArrayList<JRoi2> data = rdb.rdbGetSysRoiItemes(cat,offset) ;
        RestResult result = new RestResult();
        result.setState(0);
        result.setMessage("");
        result.setData(data);
        return result ;
    }

    //获取User ROI分类item array
    @CrossOrigin(origins = "*")
    @RequestMapping("/user")
    @ResponseBody
    public RestResult getUserRois(int uid) {
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
        ArrayList<JRoi2> data = rdb.rdbGetUserRoiItemes(uid) ;
        RestResult result = new RestResult();
        result.setState(0);
        result.setMessage("");
        result.setData(data);
        return result ;
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


    /**
     * 判断文件夹是否存在,如果不存在创建他
     * @param dirPathStr
     */
    public boolean checkDirExistsOrCreate(String dirPathStr) {
        File file = new File(dirPathStr) ;
        if (file.exists()) {
            return true ;
        } else {
            return file.mkdir();
        }
    }

    //使用ogr2ogr转shp到geojson格式 增加转换到wgs84的参数
    private boolean convertShp2Geojson(String shpfile,String geojsonfile){
        System.out.println("debug convertShp2Geojson try to convert "+shpfile+" --> "+geojsonfile);
        try
        {
            // Command to create an external process
            String command = "/usr/bin/ogr2ogr -t_srs WGS84 -f GeoJSON "+ geojsonfile+" " + shpfile;
            System.out.println("debug " + command);
            // Running the above command
            Runtime run  = Runtime.getRuntime();
            Process proc = run.exec(command);
            proc.waitFor() ;
        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
            return false ;
        }
        File gjFile = new File(geojsonfile) ;
        if( gjFile.exists()==true ){
            return true ;
        }else{
            System.out.println("convert geojson failed.");
            return false ;
        }
    }

    //使用 shpgeojson2hsegtlv 转 geojson (wgs84) 到 hseg.tlv 格式
    private boolean convertGeojson2HSegTlv(String geojsonfile,String tlvfilename){
        System.out.println("debug convertGeojson2HSegTlv try to convert "+geojsonfile+" --> "+tlvfilename);
        try
        {
            // Command to create an external process
            String command = WConfig.getSharedInstance().shpgeojson2hsegtlv + " " + geojsonfile +" " + tlvfilename;
            System.out.println("debug " + command);
            // Running the above command
            Runtime run  = Runtime.getRuntime();
            Process proc = run.exec(command);
            proc.waitFor() ;
        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
            return false ;
        }
        File gjFile = new File(geojsonfile) ;
        if( gjFile.exists()==true ){
            return true ;
        }else{
            System.out.println("convert hseg.tlv failed.");
            return false ;
        }
    }

    //读取二进制 hseg.tlv 数据文件，写入HBase ROI表
    private boolean writeHSegTlvIntoHBase( String tlvfilename ,
                                           boolean useSysRoiTable ,
                                           Integer mysqlRid )
    {
        try {
            //read binary data
            byte[] tlvdata = FileDirTool.readFileAsBytes(tlvfilename) ;
            if( tlvdata==null ){
                return false ;
            }
            //write hbase
            HBasePixelEngineHelper hhh = new HBasePixelEngineHelper() ;
            String tabname = "sys_roi" ;
            if( useSysRoiTable==false ) tabname = "user_roi" ;
            byte[] qualifier = new byte[1] ;
            qualifier[0] = 1 ;
            boolean writeOk = hhh.writeBinaryDataIntoHBase( tlvdata , tabname, "hseg.tlv" , qualifier , Bytes.toBytes(mysqlRid) );
            return writeOk ;
        }catch (Exception ex)
        {
            System.out.println("writeHSegTlvIntoHBase exception:" + ex.getMessage());
            return false;
        }
    }

    //上传shp,然后转geojson,将shp和geojson的相对路径写入数据库
    //shp shx dbf prj 写入路径在 {nginx-www-html-dir}/pe/roi/user/{yyyyMMdd}/{hhmmss}-{rand}.shp  rand为四位随机数
    //写入数据库的相对路径为 roi/user/{yyyyMMdd}/{hhmmss}-{rand}.shp/geojson
    /** ../roi/upload POST
     * @Description:上传矢量文件
     * @Author: zyp,wf
     * @Date: 2022/2/3
     * @param: [files, httpServletRequest]
     * @return:
     **/
    @CrossOrigin(origins = "*")
    @RequestMapping("/upload")
    @ResponseBody
    public RestResult upload(
            String name, //新名称，如果为空字符串则使用文件名
            String uid,
            MultipartFile[] files,
            HttpServletRequest httpServletRequest){
        System.out.println("RoiController upload ...");
        RestResult returnT = new RestResult();
        returnT.setState(0);
        returnT.setMessage("");

        {
            Date date = new Date();
            SimpleDateFormat datetimeFormat1 = new SimpleDateFormat("yyyyMMdd");
            String yyyyMMddStr = datetimeFormat1.format(date);
            SimpleDateFormat datetimeFormat2 = new SimpleDateFormat("HHmmss");
            String hhmmssStr = datetimeFormat2.format(date);
            String randStr = String.format("%04d",new Random().nextInt(9999)) ;
            String newFileName = hhmmssStr+"-"+randStr ;

            if( files.length !=4 ){
                returnT.setState(9);
                returnT.setMessage("files count not 4.");
                return returnT ;
            }

            //check roi/ dir ok
            String roidirPathStr = WConfig.getSharedInstance().pedir + "/roi/" ;
            boolean roidirOk = checkDirExistsOrCreate(roidirPathStr) ;
            if( roidirOk==false ){
                returnT.setState(9);
                returnT.setMessage("bad roi dir.");
                return returnT ;
            }

            //check roi/user dir ok
            String roiUserDirPathStr = WConfig.getSharedInstance().pedir + "/roi/user/" ;
            boolean roiUserDirOk = checkDirExistsOrCreate(roiUserDirPathStr) ;
            if( roiUserDirOk==false ){
                returnT.setState(9);
                returnT.setMessage("bad roi/user dir.");
                return returnT ;
            }

            //check roi/user/yyyyMMdd dir ok
            String roiUserYmdDirPathStr = WConfig.getSharedInstance().pedir + "/roi/user/"+yyyyMMddStr+"/" ;
            boolean dir3Ok = checkDirExistsOrCreate(roiUserYmdDirPathStr) ;
            if( dir3Ok==false ){
                returnT.setState(9);
                returnT.setMessage("bad roi/user/{yyyyMMdd} dir.");
                return returnT ;
            }


            //查询是否够四个文件，够了入库
            boolean shpBoo = false;
            boolean dbfBoo = false;
            boolean prjBoo = false;
            boolean shxBoo = false;
            String theShpFilepath = "" ;
            String theShpFilename = "" ;
            for(int ifile = 0 ; ifile < files.length;++ ifile ){
                String tempfilename = files[ifile].getOriginalFilename() ;
                if(tempfilename.endsWith(".shp")){
                    String newfilepath = roiUserYmdDirPathStr+newFileName+".shp" ;
                    shpBoo = writeUploadFileToFile( files[ifile] , newfilepath );
                    theShpFilepath = newfilepath ;
                    theShpFilename = tempfilename ;
                }
                if(tempfilename.endsWith(".dbf")){
                    String newfilepath = roiUserYmdDirPathStr+newFileName+".dbf" ;
                    dbfBoo = writeUploadFileToFile( files[ifile] , newfilepath );
                }
                if(tempfilename.endsWith(".prj")){
                    String newfilepath = roiUserYmdDirPathStr+newFileName+".prj" ;
                    prjBoo = writeUploadFileToFile( files[ifile] , newfilepath );
                }
                if(tempfilename.endsWith(".shx")){
                    String newfilepath = roiUserYmdDirPathStr+newFileName+".shx" ;
                    shxBoo = writeUploadFileToFile( files[ifile] , newfilepath );
                }
            }

            if( !shpBoo || !shxBoo || !prjBoo || !dbfBoo )
            {
                returnT.setState(9);
                returnT.setMessage("failed to write shp "+shpBoo + ", shx "+shxBoo + ", dbf "+dbfBoo + ", prj "+prjBoo);
                return returnT ;
            }

            //new geojson filepath
            String newGeojsonfilepath = roiUserYmdDirPathStr+newFileName+".geojson" ;//fullpath
            boolean geojsonOk = convertShp2Geojson(theShpFilepath,newGeojsonfilepath) ;

            //convert failed
            if( geojsonOk==false ){
                returnT.setState(9);
                returnT.setMessage("convert geojson failed.");
                return returnT ;
            }

            //Convert and write hseg.tlv
            String tlvfilename = roiUserYmdDirPathStr+newFileName+".hseg.tlv" ;//fullpath
            boolean tlvOk = convertGeojson2HSegTlv( newGeojsonfilepath, tlvfilename );
            if( tlvOk==false ){
                returnT.setState(9);
                returnT.setMessage("failed to write to hseg.tlv.");
                return returnT ;
            }


            //write info to db //数据入库
            if( name.compareTo("")==0 ){
                name = theShpFilename ;
            }
            if( name.length()>20 ){
                name = name.substring(0,20) ;
            }
            String shpRelPath = "roi/user/"+yyyyMMddStr+"/" + newFileName+".shp";
            String geojsonRelPath = "roi/user/"+yyyyMMddStr+"/" + newFileName+".geojson";
            JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
            int newRid = rdb.rdbNewRoi2(name ,shpRelPath,geojsonRelPath, Integer.valueOf(uid) ) ;
            if( newRid< 0 ){
                //bad
                returnT.setState(9);
                returnT.setMessage("failed to write to mysql.");
                return returnT ;
            }



            //read tlv and write into HBase
            boolean tlvHbaseOk = writeHSegTlvIntoHBase( tlvfilename , false , newRid) ;
            if( tlvHbaseOk==false ){
                returnT.setState(9);
                returnT.setMessage("failed to write hseg.tlv into HBase.");

                //这里需要删除mysql中的记录，以后有时间在加 2022-3-6
                //...

                return returnT ;
            }else{
                //写入Hbase成功以后应该删除tlv文件，以后有时间在加 2022-3-6
                //...
            }


            JRoi2 newRoiObj = rdb.rdbGetUserRoiItem(newRid);
            if( newRoiObj==null ){
                returnT.setState(9);
                returnT.setMessage("failed to get new roi object.");
                return returnT ;
            }

            returnT.setState(0);
            returnT.setData( newRoiObj );
            return returnT;
        }
    }

    // 上传用户手绘Geojson感兴趣区
    // save a geojson into storage
    @PostMapping(value="/newgeojson")
    @CrossOrigin(origins = "*")
    @ResponseBody
    public RestResult newGeoJson( int uid,
                                   String name,
                                   String geojson )
    {
        RestResult result = new RestResult() ;

        //write into file
        Date date = new Date();
        SimpleDateFormat datetimeFormat1 = new SimpleDateFormat("yyyyMMdd");
        String yyyyMMddStr = datetimeFormat1.format(date);
        SimpleDateFormat datetimeFormat2 = new SimpleDateFormat("HHmmss");
        String hhmmssStr = datetimeFormat2.format(date);
        String randStr = String.format("%04d",new Random().nextInt(9999)) ;
        String newFileName = hhmmssStr+"-"+randStr ;

        //check roi/ dir ok
        String roidirPathStr = WConfig.getSharedInstance().pedir + "/roi/" ;
        boolean roidirOk = checkDirExistsOrCreate(roidirPathStr) ;
        if( roidirOk==false ){
            result.setState(9);
            result.setMessage("bad roi dir.");
            return result ;
        }

        //check roi/user dir ok
        String roiUserDirPathStr = WConfig.getSharedInstance().pedir + "/roi/user/" ;
        boolean roiUserDirOk = checkDirExistsOrCreate(roiUserDirPathStr) ;
        if( roiUserDirOk==false ){
            result.setState(9);
            result.setMessage("bad roi/user dir.");
            return result ;
        }

        //check roi/user/yyyyMMdd dir ok
        String roiUserYmdDirPathStr = WConfig.getSharedInstance().pedir + "/roi/user/"+yyyyMMddStr+"/" ;
        boolean dir3Ok = checkDirExistsOrCreate(roiUserYmdDirPathStr) ;
        if( dir3Ok==false ){
            result.setState(9);
            result.setMessage("bad roi/user/{yyyyMMdd} dir.");
            return result ;
        }

        if( name.length()>20 ) name = name.substring(0,20) ;
        String absfilepath = roiUserYmdDirPathStr + newFileName + ".geojson" ;
        try{
            //将geojson对象写入文件
            PrintWriter out = new PrintWriter(absfilepath);
            out.print(geojson) ;
            out.close();

            //转换tlv //Convert and write hseg.tlv
            String tlvfilename = roiUserYmdDirPathStr+newFileName+".hseg.tlv" ;//fullpath
            boolean tlvOk = convertGeojson2HSegTlv( absfilepath, tlvfilename );
            if( tlvOk==false ){
                throw new Exception("failed to write hseg.tlv.") ;
            }


            String relfilepath = "roi/user/"+yyyyMMddStr+"/"+newFileName+".geojson" ;
            JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
            int newRid = rdb.rdbNewRoi2(name,"",relfilepath,uid) ;
            if( newRid<0 ){
                //bad
                result.setState(9);
                result.setMessage("failed to write db.");
            }else{

                //读取tlv 写入HBase
                boolean tlvHbaseOk = writeHSegTlvIntoHBase( tlvfilename , false , newRid) ;
                if( tlvHbaseOk==false ){
                    throw new Exception("failed to write hseg.tlv into HBase.");

                    //这里需要删除mysql中的记录，以后有时间在加 2022-3-6
                    //...
                }else{
                    //写入Hbase成功以后应该删除tlv文件，以后有时间在加 2022-3-6
                    //...
                }

                result.setState(0);
                result.setMessage("");
                JRoi2 newRoi = rdb.rdbGetUserRoiItem(newRid);
                result.setData(newRoi);
            }
        }catch (Exception e){
            e.printStackTrace();
            result.setState(9);
            result.setMessage(e.getMessage());
        }
        return result ;
    }


    // remove a user ROI
    @PostMapping(value="/remove")
    @CrossOrigin(origins = "*")
    @ResponseBody
    public RestResult removeRoi( int rid )
    {
        RestResult result = new RestResult() ;
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
        JRoi2 roi = rdb.rdbGetUserRoiItem(rid) ;
        if( roi!=null ){
            //删除物理文件
            if( roi.shp!=null && roi.shp.length() > 1 ){
                String shpfilepath = WConfig.getSharedInstance().pedir + roi.shp ;
                File file = new File(shpfilepath) ; file.delete() ;
                String shxfilepath = shpfilepath.substring(0, shpfilepath.length()-3 ) + "shx" ;
                File file1 = new File(shxfilepath) ; file1.delete() ;
                String dbffilepath = shpfilepath.substring(0, shpfilepath.length()-3 ) + "dbf" ;
                File file2 = new File(dbffilepath) ; file2.delete() ;
                String prjfilepath = shpfilepath.substring(0, shpfilepath.length()-3 ) + "prj" ;
                File file3 = new File(prjfilepath) ; file3.delete() ;
            }
            if( roi.geojson!=null && roi.geojson.length()>1 ){
                String geojsonfilepath = WConfig.getSharedInstance().pedir + roi.geojson ;
                File file = new File(geojsonfilepath) ;
                file.delete() ;
            }
        }
        rdb.rdbRemoveUserRoi(rid) ;
        result.setState(0);
        result.setMessage("");
        return result ;
    }
}
