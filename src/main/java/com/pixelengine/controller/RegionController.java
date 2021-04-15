package com.pixelengine.controller;


import com.pixelengine.DAO.RegionDAO;
import com.pixelengine.DAO.StyleDAO;
import com.pixelengine.DTO.RegionDTO;
import com.pixelengine.DataModel.RestResult;
import com.pixelengine.JUser;
import com.pixelengine.WConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.swing.plaf.synth.Region;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/region")
public class RegionController {
    @Autowired
    RegionDAO dao ;

    //get user region list
    @CrossOrigin
    @RequestMapping("/userlist")
    @ResponseBody
    public RestResult userList(String userid) {
        List<RegionDTO> rlist = dao.findAllByUserid( Long.parseLong(userid)) ;
        RestResult returnT = new RestResult();
        returnT.setState(0);
        returnT.setMessage("");
        returnT.setData(rlist);

        return returnT ;
    }


    //key filter
    @CrossOrigin
    @RequestMapping("/findByName")
    @ResponseBody
    public RestResult findByName(String userid, String key) {
        List<RegionDTO> rlist = dao.findByName( Long.parseLong(userid),key) ;
        RestResult returnT = new RestResult();
        returnT.setState(0);
        returnT.setMessage("");
        returnT.setData(rlist);

        return returnT ;
    }

    //modify region name
    @CrossOrigin
    @PostMapping("/updatename")
    @ResponseBody
    public RestResult updateName(String rid,String name) {
        RegionDTO oldregion = dao.getOne(Long.parseLong(rid)) ;
        oldregion.setName(name);
        RegionDTO newregion = dao.save(oldregion) ;
        RestResult returnT = new RestResult();
        returnT.setState(0);
        returnT.setMessage("");
        returnT.setData(newregion);
        return returnT ;
    }


    //remove region
    @CrossOrigin
    @RequestMapping("/remove")
    @ResponseBody
    public RestResult remove(String rid) {
        RegionDTO tregion = dao.getOne( Long.parseLong(rid)) ;
        if( tregion.getShp() != null && tregion.getShp().equals("")==false ){
            File shpfile = new File(tregion.getShp()) ;
            shpfile.delete() ;
            String dbfname = tregion.getShp().replace(".shp",".dbf") ;
            String prjname = tregion.getShp().replace(".shp",".prj") ;
            String shxname = tregion.getShp().replace(".shp",".shx") ;
            new File(dbfname).delete() ;
            new File(prjname).delete() ;
            new File(shxname).delete() ;
        }

        if( tregion.getGeojson() != null && tregion.getGeojson().equals("")==false){
            new File(tregion.getGeojson()).delete() ;
        }


        dao.deleteById( Long.parseLong(rid));
        RestResult returnT = new RestResult();
        returnT.setState(0);
        returnT.setMessage("");
        return returnT ;
    }


    //上传shp，转geojson，并入库
    /**
     * @Description:上传矢量文件
     * @Author: zyp
     * @Date: 2021/1/15
     * @param: [files, httpServletRequest]
     * @return: com.piesat.utils.ReturnT
     **/
    @CrossOrigin
    @RequestMapping("/upload")
    @ResponseBody
    public RestResult upload(
            @RequestHeader("token") String token,
            MultipartFile[] files,  HttpServletRequest httpServletRequest){
        System.out.println("uploading ...");
        RestResult returnT = new RestResult();
        returnT.setState(0);
        returnT.setMessage("");

        JUser tempUser = JUser.getUserByToken(token) ;
        if( tempUser == null ){
            returnT.setData(1);
            returnT.setMessage("没有用户登录信息");
            return returnT ;
        }else{
            Date date = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String datestr = sdf.format(date);

            String roiRootDir = WConfig.sharedConfig.pedir + "/roi/" ;
            File roiRootDirFileObj = new File(roiRootDir) ;
            if( roiRootDirFileObj.exists()==false ){
                roiRootDirFileObj.mkdir() ;
            }
            String relativeUploadDateDir = "/roi/" + datestr + "/" ;
            String absUploadDateDir = WConfig.sharedConfig.pedir + relativeUploadDateDir ;
            String shp = "";
            String shpFilePath = "" ;
            for(int ifile = 0 ; ifile < files.length;++ ifile ){
                String name = uploadFile(files[ifile],absUploadDateDir);
                System.out.println(name); ;
            }

            //查询是否够四个文件，够了入库
            boolean shpBoo = false;
            boolean dbfBoo = false;
            boolean prjBoo = false;
            boolean shxBoo = false;
            File rootfile = new File(absUploadDateDir);
            if(rootfile.exists()){

                File[] files_server = rootfile.listFiles();
                for(File fil: files_server){
                    System.out.println("---"+fil.getName());
                    if(fil.getName().endsWith(".shp")){
                        shp = fil.getName();
                        shpFilePath = fil.getPath() ;
                        shpBoo = true;
                    }
                    if(fil.getName().endsWith(".dbf")){
                        dbfBoo = true;
                    }
                    if(fil.getName().endsWith(".prj")){
                        prjBoo = true;
                    }
                    if(fil.getName().endsWith(".shx")){
                        shxBoo = true;
                    }
                }
                if(shpBoo && dbfBoo && prjBoo && shxBoo){
                    System.out.println(""+shpBoo +dbfBoo + prjBoo + shxBoo);
                    //将shp文件转换为geojson文件
                    String shpFileNameWithoutExtName = shp.split(".shp")[0];

                    String dbFilePath = relativeUploadDateDir + shpFileNameWithoutExtName +".geojson" ;
                    String geojsonFilePath = WConfig.sharedConfig.pedir + dbFilePath ;
                    int itry = 1;
                    while( (new File(geojsonFilePath)).exists()==true ){
                        dbFilePath = relativeUploadDateDir + shpFileNameWithoutExtName + "-" + itry +".geojson" ;
                        geojsonFilePath = WConfig.sharedConfig.pedir + dbFilePath ;
                        ++itry ;
                    }
                    System.out.println("used geojson file:" + geojsonFilePath);
                    //do convert shp to geojson
                    boolean convertOk = convertShp2Geojson(shpFilePath,geojsonFilePath) ;
                    if( convertOk==false ){
                        //failed.
                        returnT.setState(1);
                        returnT.setMessage("convert geojson failed.");
                    }else{
                        //数据入库
                        RegionDTO region1 = new RegionDTO() ;
                        region1.setGeojson(dbFilePath);
                        region1.setShp(shpFilePath);
                        region1.setName(shpFileNameWithoutExtName);
                        region1.setUid( tempUser.uid );//

                        RegionDTO newregion = dao.save(region1) ;
                        returnT.setData(newregion);
                    }
                }else{
                    System.out.println("lack of some files:");
                    System.out.println("shp "+ shpBoo);
                    System.out.println("shp "+ dbfBoo);
                    System.out.println("shp "+ prjBoo);
                    System.out.println("shp "+ shxBoo);
                }
            }
            return returnT;
        }
    }
    //write file into file system.
    public static String uploadFile(MultipartFile file,String dateDir){
        String fileName = file.getOriginalFilename();
        File targetFile = new File(dateDir);
        //第一步：判断文件是否为空
        if(!file.isEmpty()){
            //第二步：判断目录是否存在   不存在：创建目录
            if(!targetFile.exists()){
                targetFile.mkdirs();
            }
            //第三部：通过输出流将文件写入硬盘文件夹并关闭流
            BufferedOutputStream stream = null;
            try {
                stream = new BufferedOutputStream(new FileOutputStream(dateDir+fileName));
                stream.write(file.getBytes());
                stream.flush();
            }catch (IOException e){
                e.printStackTrace();
            }finally {
                try {
                    if (stream != null) stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return fileName;
    }

    //write String into file system.
    public static boolean writeFile(String content,String filePath){
        //第一步：判断文件是否为空
        if(!filePath.isEmpty()){
            File targetFile = new File(filePath) ;
            File targetdir = new File(targetFile.getParent()) ;
            //第二步：判断目录是否存在   不存在：创建目录
            if(!targetdir.exists()){
                targetdir.mkdirs();
            }
            //第三部：通过输出流将文件写入硬盘文件夹并关闭流
            BufferedOutputStream stream = null;
            try {
                stream = new BufferedOutputStream(new FileOutputStream(filePath));
                stream.write(content.getBytes());
                stream.flush();
            }catch (IOException e){
                e.printStackTrace();
                return false ;
            }finally {
                try {
                    if (stream != null) stream.close();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false ;
                }
            }
        }else{
            return false ;
        }
    }


    private boolean convertShp2Geojson(String shpfile,String geojsonfile){
        System.out.println("try to convert "+shpfile+" --> "+geojsonfile);
        try
        {
            // Command to create an external process
            String command = "/usr/bin/ogr2ogr -t_srs WGS84 -f GeoJSON "+ geojsonfile+" " + shpfile;
            System.out.println(command);
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



    //保存geojson，并入库
    /**wf
     **/
    @CrossOrigin
    @PostMapping("/savegeojson2")
    @ResponseBody
    public RestResult saveGeoJson2(
            @RequestHeader("token") String token,
            String content,String name){

        System.out.println("saving geojson ...");
        RestResult returnT = new RestResult();

        JUser tempUser = JUser.getUserByToken(token) ;
        if( tempUser == null ){
            returnT.setData(1);
            returnT.setMessage("没有用户登录信息");
            return returnT ;
        }
        else{
            Date date = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String datestr = sdf.format(date);
            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMddhhmmss");
            String datestr1 = sdf1.format(date);

            String roiRootDir = WConfig.sharedConfig.pedir + "/roi/" ;
            File roiRootDirFileObj = new File(roiRootDir) ;
            if( roiRootDirFileObj.exists()==false ){
                roiRootDirFileObj.mkdir() ;
            }
            String dbroidir =  "/roi/" +datestr +"/";
            String gjFileNameWithoutExtName = (new String()).format("roi-%d-%s",tempUser.uid , datestr1);
            String dbFilePath = dbroidir + gjFileNameWithoutExtName +".geojson" ;
            String geojsonFilePath = WConfig.sharedConfig.pedir +dbFilePath ;
            int itry = 1;
            while( (new File(geojsonFilePath)).exists()==true ){
                dbFilePath = dbroidir + gjFileNameWithoutExtName +"-" +itry+".geojson" ;
                geojsonFilePath =WConfig.sharedConfig.pedir + dbFilePath ;
                ++itry ;
            }
            System.out.println("used geojson file:" + geojsonFilePath);
            writeFile( content , geojsonFilePath) ;

            //数据入库
            RegionDTO region1 = new RegionDTO() ;
            region1.setGeojson(dbFilePath);
            region1.setShp("");
            region1.setName(name);
            region1.setUid(  tempUser.uid );//

            RegionDTO newregion = dao.save(region1) ;
            returnT.setData(newregion);

            return returnT;
        }
    }


    //保存geojson，并入库
    /**wf
     **/
    @CrossOrigin
    @PostMapping("/savegeojson")
    @ResponseBody
    public RestResult saveGeoJson(
            String userid,
            String content,String name){

        System.out.println("saving geojson ...");
        RestResult returnT = new RestResult();

        {
            Date date = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String datestr = sdf.format(date);
            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMddhhmmss");
            String datestr1 = sdf1.format(date);

            String roiRootDir = WConfig.sharedConfig.pedir + "/roi/" ;
            File roiRootDirFileObj = new File(roiRootDir) ;
            if( roiRootDirFileObj.exists()==false ){
                roiRootDirFileObj.mkdir() ;
            }
            String dbroidir =  "/roi/" +datestr +"/";
            String gjFileNameWithoutExtName = (new String()).format("roi-%s-%s", userid , datestr1);
            String dbFilePath = dbroidir + gjFileNameWithoutExtName +".geojson" ;
            String geojsonFilePath = WConfig.sharedConfig.pedir +dbFilePath ;
            int itry = 1;
            while( (new File(geojsonFilePath)).exists()==true ){
                dbFilePath = dbroidir + gjFileNameWithoutExtName +"-" +itry+".geojson" ;
                geojsonFilePath =WConfig.sharedConfig.pedir + dbFilePath ;
                ++itry ;
            }
            System.out.println("used geojson file:" + geojsonFilePath);
            writeFile( content , geojsonFilePath) ;

            //数据入库
            RegionDTO region1 = new RegionDTO() ;
            region1.setGeojson(dbFilePath);
            region1.setShp("");
            region1.setName(name);
            region1.setUid(  Integer.valueOf(userid) );//

            RegionDTO newregion = dao.save(region1) ;
            returnT.setData(newregion);

            return returnT;
        }
    }





}
