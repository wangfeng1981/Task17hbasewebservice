package com.pixelengine.controller;


import com.pixelengine.DAO.StyleDAO;
import com.pixelengine.DTO.StyleDTO;
import com.pixelengine.DataModel.JStyleDbObject;
import com.pixelengine.DataModel.RestResult;
import com.pixelengine.DataModel.WConfig;
import com.pixelengine.JRDBHelperForWebservice;
import com.pixelengine.DataModel.JUser;
import com.pixelengine.tools.FileDirTool;
import org.apache.commons.net.ntp.TimeStamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.yaml.snakeyaml.tokens.DirectiveToken;

import java.io.File;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.sql.Timestamp;//2022-5-31

@RestController
@RequestMapping(value="/style")
@CrossOrigin(origins = "*")
public class StyleController {
    @Autowired
    StyleDAO styleDao ;


    @PostMapping(value="/new2")
    @CrossOrigin(origins = "*")
    public RestResult styleNew(@RequestHeader("token") String token,
                               String stylecontent , String description )
    {
        RestResult result = new RestResult() ;
        JUser tempUser = JUser.getUserByToken(token) ;
        if( tempUser == null ){
            result.setData(1);
            result.setMessage("没有用户登录信息");
            return result ;
        }else{
            StyleDTO s1 = new StyleDTO() ;
            s1.setStyleContent(stylecontent);
            s1.setDescription(description);
            s1.setUserid(  (long)tempUser.uid );
            s1.setCreatetime(Calendar.getInstance().getTime());
            s1.setUpdatetime(Calendar.getInstance().getTime());
            StyleDTO newStyle = styleDao.save(s1) ;

            result.setState(0);
            result.setData(newStyle);
            return result;
        }
    }

    @PostMapping(value="/new")
    @CrossOrigin(origins = "*")
    @ResponseBody
    public RestResult styleNewOld( String userid,
                               String stylecontent , String description )
    {
        RestResult result = new RestResult() ;

        {
            StyleDTO s1 = new StyleDTO() ;
            s1.setStyleContent(stylecontent);
            s1.setDescription(description);
            s1.setUserid(  Long.valueOf(userid) );
            s1.setCreatetime(Calendar.getInstance().getTime());
            s1.setUpdatetime(Calendar.getInstance().getTime());
            StyleDTO newStyle = styleDao.save(s1) ;

            result.setState(0);
            result.setData(newStyle);
            return result;
        }
    }

    //update 2022-6-12
    @PostMapping(value="/edit")
    @CrossOrigin(origins = "*")
    public RestResult styleEdit(String styleid,
                                String stylecontent,
                                String description )
    {
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
        boolean isok = rdb.rdbUpdateStyle(Integer.valueOf(styleid),
                stylecontent,
                description) ;
        if( isok ==true ){
            JStyleDbObject styleobj = rdb.rdbGetStyle2(Integer.parseInt(styleid));
            styleobj.filename = generateStyleFile(
                    String.valueOf( styleobj.styleid ) ,
                    styleobj.styleContent,
                    styleobj.updatetime) ;
            RestResult result = new RestResult() ;
            result.setState(0);
            result.setData(styleobj);
            return result ;
        }else{
            RestResult result = new RestResult() ;
            result.setState(1);
            result.setMessage("Failed to update style.");
            return result ;
        }
    }


    //2022-5-31
    //检查是否有 {pedir}/style/{}-{}.json 文件，如果有返回json文件相对路径，反之从数据库读取并写入该文件
    public String generateStyleFile(String styleid,String styleContent,String updateDatetimeStr)
    {
        java.sql.Timestamp ts = java.sql.Timestamp.valueOf(updateDatetimeStr) ;
        String tsStr = String.valueOf( ts.getTime()/1000 );
        String relfilename = "styles/"+styleid+"-"+tsStr+".json" ;
        String fullfilename = WConfig.getSharedInstance().pedir+relfilename ;
        File jfile = new File(fullfilename) ;
        if( jfile.exists()==false) {
            String styleDir = WConfig.getSharedInstance().pedir+"styles" ;
            if( FileDirTool.checkDirExistsOrCreate(styleDir)==false ){
                System.out.println("generateStyleFile failed to mkdir "+ styleDir);
                return "" ;
            }
            if( FileDirTool.writeToTextFile(fullfilename,styleContent) == false )
            {
                System.out.println("generateStyleFile failed to write "+ fullfilename);
                return "" ;
            }else{
                System.out.println("generateStyleFile write ok:"+ fullfilename);
            }
        }
        return relfilename;
    }


    //2022-5-31 write style content into file, and return the relative path.
    //first check if the file exists, if not write it into file with timestamp.
    @GetMapping(value="/detail/{styleid}")
    @CrossOrigin(origins = "*")
    public RestResult styleGet(@PathVariable("styleid") String styleid)
    {
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        JStyleDbObject styleobj = rdb.rdbGetStyle2(Integer.parseInt(styleid));
        styleobj.filename = generateStyleFile(
                String.valueOf( styleobj.styleid ) ,
                styleobj.styleContent,
                styleobj.updatetime) ;

        RestResult result = new RestResult() ;
        result.setState(0);
        result.setData(styleobj);
        return result;
    }

    @GetMapping(value="/remove/{styleid}")
    @CrossOrigin(origins = "*")
    public RestResult styleRemove(@PathVariable("styleid") String styleid)
    {
        styleDao.deleteById( Long.parseLong(styleid));
        RestResult result = new RestResult() ;
        result.setState(0);
        return result;
    }

    @GetMapping(value="/list/{userid}")
    @CrossOrigin(origins = "*")
    public RestResult styleList(@PathVariable("userid") String userid)
    {
        List<StyleDTO> allstyle = styleDao.findAllByUserid( Long.parseLong(userid)) ;
        RestResult result = new RestResult() ;
        result.setState(0);
        result.setData(allstyle);
        return result;
    }



//    <ows:Operation name="GetLegendGraphic">
//    <ows:DCP>
//        <ows:HTTP>
//            <ows:Get xlink:href="{task17_api_root}style/legend/"></ows:Get>
//        </ows:HTTP>
//    </ows:DCP>
//    </ows:Operation>
    //2022-5-20
    // /pe/style/legend/  有待完善2022-5-20
    @ResponseBody
    @RequestMapping(value="/legend/",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public ResponseEntity<byte[]> getLegend(String styleid)
    {
        System.out.println("/style/legend/");
        System.out.println("styleid:" + styleid) ;
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        byte[] pngbytes = FileDirTool.readFileAsBytes("placeholder.png") ;
        return new ResponseEntity<byte[]>(pngbytes, headers, HttpStatus.OK);
    }

}
