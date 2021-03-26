package com.pixelengine.controller;


import com.pixelengine.DAO.RegionDAO;
import com.pixelengine.DAO.StyleDAO;
import com.pixelengine.DTO.RegionDTO;
import com.pixelengine.DataModel.RestResult;
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
    public RestResult upload(MultipartFile[] files, String userid, HttpServletRequest httpServletRequest){
        System.out.println("uploading ...");
        RestResult returnT = new RestResult();
        returnT.setState(0);
        returnT.setMessage("");

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
        String da = sdf.format(date);
        String uploadPath = WConfig.sharedConfig.uploadRegionPath + "/" +da +"/";
        String shp = "";
        String shpFilePath = "" ;
        for(int ifile = 0 ; ifile < files.length;++ ifile ){
            String name = uploadFile(files[ifile],uploadPath);
            System.out.println(name); ;
        }

        //查询是否够四个文件，够了入库
        boolean shpBoo = false;
        boolean dbfBoo = false;
        boolean prjBoo = false;
        boolean shxBoo = false;
        File rootfile = new File(uploadPath);
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
                String geojsonFilePath = uploadPath + shpFileNameWithoutExtName +".geojson" ;
                int itry = 1;
                while( (new File(geojsonFilePath)).exists()==true ){
                    geojsonFilePath = uploadPath + shpFileNameWithoutExtName +"-" +itry  +".geojson" ;
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
                    region1.setGeojson(geojsonFilePath);
                    region1.setShp(shpFilePath);
                    region1.setName(shpFileNameWithoutExtName);
                    region1.setUid( Integer.parseInt(userid));//

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

    public static String uploadFile(MultipartFile file,String filePath){
        String fileName = file.getOriginalFilename();
        File targetFile = new File(filePath);
        //第一步：判断文件是否为空
        if(!file.isEmpty()){
            //第二步：判断目录是否存在   不存在：创建目录
            if(!targetFile.exists()){
                targetFile.mkdirs();
            }
            //第三部：通过输出流将文件写入硬盘文件夹并关闭流
            BufferedOutputStream stream = null;
            try {
                stream = new BufferedOutputStream(new FileOutputStream(filePath+fileName));
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






}
