package com.pixelengine.controller;
//update 2022-4-25 task17_api_root
//2022-5-11
//2022-5-19

import com.google.gson.Gson;
import com.pixelengine.*;
import com.pixelengine.DataModel.*;
import com.pixelengine.tools.FileDirTool;
import com.pixelengine.tools.ScriptsGetterTool;
import com.pixelengine.TileComputeResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Optional;

@RestController
public class ScriptsController {

//    @ResponseBody
//    @RequestMapping(value="/scripts/user/new/{uid}",method= RequestMethod.GET)
//    @CrossOrigin(origins = "*")
//    public ResponseEntity<byte[]> scriptnew(@PathVariable String uid , String type)
//    {//deprecated 2022-5-10
//
//    }

    //2022-5-10
    @ResponseBody
    @RequestMapping(value="/scripts/new2",method= RequestMethod.POST)
    @CrossOrigin(origins = "*")
    public RestResult new2(String uid , String script)
    {
        System.out.println("/scripts/new2");

        RestResult rr = new RestResult();
        rr.setData(null);
        rr.setState(0);
        rr.setMessage("");

        FileDirTool.FileNamerResult fnr = FileDirTool.buildDatetimeSubdirAndFilename(
                WConfig.getSharedInstance().pedir,
                "scripts/user" , "s" , ".js"
        ) ;
        if( fnr.state != 0 ){
            rr.setMessage(fnr.message);
            rr.setState(fnr.state);
            return rr ;
        }

        boolean wok = FileDirTool.writeToFile(fnr.data.absfilename,script) ;
        if( wok==false ){
            rr.setState(11);
            rr.setMessage("Failed to write script file.");
            return rr ;
        }

        int uid2 = Integer.parseInt(uid) ;

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        int newsid = rdb.rdbNewUserScript(uid2,fnr.data.relfilename) ;

        if( newsid < 0 ){
            rr.setState(12);
            rr.setMessage("Failed to insert to mysql.");
            return rr ;
        }

        JScript sc = rdb.rdbGetScript( newsid ) ;
        if( sc==null ){
            rr.setState(13);
            rr.setMessage("Failed to find script by "+newsid);
            return rr;
        }
        rr.setData(sc);
        rr.setState(0);
        return rr;
    }


    /// 用户脚本列表 2022-5-11
    @ResponseBody
    @RequestMapping(value="/scripts/userlist",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult scriptlist( String uid)
    {
        // return maximum 50 scripts from newest to oldest.
        RestResult rr = new RestResult() ;

        System.out.println("/scripts/userlist");
        int uid2 = Integer.parseInt(uid);
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        ArrayList<JScript> list = rdb.rdbGetUserScriptList(uid2) ;
        if( list==null ){
            rr.setState(1);
            rr.setMessage("failed to get user script list.");
            return rr ;
        }
        rr.setState(0);
        rr.setData(list);
        return rr;
    }


//deprecated 2022-5-11
//    @ResponseBody
//    @RequestMapping(value="/scripts/user/{uid}",method= RequestMethod.GET)
//    @CrossOrigin(origins = "*")
//    public ResponseEntity<byte[]> scriptlist(@PathVariable String uid)
//    {
//        System.out.println("/scripts/user/{uid}");
//        int uid2 = Integer.parseInt(uid);
//        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
//        String outjson = rdb.rdbGetUserScriptListJson(uid2) ;
//
//        final HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        return new ResponseEntity<byte[]>( outjson.getBytes(), headers, HttpStatus.OK);
//    }

    //获取js脚本中的内容 relPath 是相对路径 scrips/...
    public static String sgetScriptContent( String relPath )
    {
        String fullpath = WConfig.getSharedInstance().pedir + relPath ;
        String data = "";
        try{
            data = new String(Files.readAllBytes(Paths.get(fullpath)));
        }catch (Exception ex){
            System.out.println("sgetScriptContent exception:"+ex.getMessage());
            return null ;
        }
        return data;
    }

    //获取js脚本中的内容 relPath 是相对路径 scrips/...
    private String getScriptContent( String relPath )
    {
        return ScriptsController.sgetScriptContent(relPath);
    }


    //update 2022-2-5
    @ResponseBody
    @RequestMapping(value="/scripts/{sid}",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult scriptdetail(@PathVariable String sid)
    {
        System.out.println("/scripts/" + sid);
        JRDBHelperForWebservice rdb =new JRDBHelperForWebservice();
        JScript sc = rdb.rdbGetScript(Integer.parseInt(sid)) ;
        if( sc==null ){
            RestResult rr = new RestResult();
            rr.setData(null);
            rr.setState(9);
            rr.setMessage("not find script by sid "+sid);
            return rr;
        }else
        {
            sc.scriptContent = getScriptContent( sc.jsfile) ;
            RestResult rr = new RestResult();
            rr.setData(sc);
            rr.setState(0);
            rr.setMessage("");
            return rr;
        }
    }

    //create 2022-2-5
    @ResponseBody
    @RequestMapping(value="/scripts/{sid}/wmts/WMTSCapabilities.xml",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public ResponseEntity<byte[]> scriptWMTSCaps(@PathVariable String sid)
    {
        System.out.println("/scripts/" + sid + "/wmts/WMTSCapabilities.xml");
        String scriptWmtsTemplate = WConfig.getSharedInstance().scriptwmts ;
        try{
            JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
            JScript scriptObj = rdb.rdbGetScript( Integer.valueOf(sid)) ;
            if( scriptObj!=null )
            {
                String xmlContent = FileDirTool.readFileAsString(scriptWmtsTemplate) ;
                String xmlContent2 = xmlContent.replace("{sid}",sid);
                //host
                xmlContent2 = xmlContent2.replace("{host}", WConfig.getSharedInstance().host );
                //port
                xmlContent2 = xmlContent2.replace("{port}", WConfig.getSharedInstance().port);
                //script-utime
                xmlContent2 = xmlContent2.replace("{utime}",
                        String.valueOf(Timestamp.valueOf( scriptObj.utime).getTime()/1000) ) ;

                //task17_api_root
                xmlContent2 = xmlContent2.replace("{task17_api_root}",  WConfig.getSharedInstance().task17_api_root  ) ;

                //return xml
                final HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.TEXT_XML);
                return new ResponseEntity<byte[]>(xmlContent2.getBytes(), headers, HttpStatus.OK);
            }
            else{
                System.out.println("Error : not found script for "+sid );
                final HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.TEXT_PLAIN);
                return new ResponseEntity<byte[]>((" not found script for sid: "+sid).getBytes(), headers, HttpStatus.NOT_FOUND ) ;
            }
        }catch (Exception ex ){
            System.out.println("Error : script wmts exception .");
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            return new ResponseEntity<byte[]>(("scriptWMTSCaps exception "+ex.getMessage()).getBytes(), headers, HttpStatus.NOT_FOUND ) ;
        }
    }


    ///成功返回具体值，失败返回0
    private int getStyleIdFromJsScript(String script)
    {
        int pos1 = script.indexOf("function setStyle()") ;
        if( pos1>=0 ){
            int pos2 = pos1 + "function setStyle()".length() ;
            int pos3 = script.indexOf("}",pos2) ;
            if( pos3>1 ){
                int pos4 = pos3 - 1 ;
                try{
                    String returnStr = script.substring(pos2 , pos4) ;
                    String r1 = returnStr.replace("{","") ;
                    String r2 = r1.replace("return","") ;
                    String r3 = r2.replace(";","") ;
                    String r4 = r3.replace(" ","") ;
                    r4 = r4.replace("\n","") ;
                    r4 = r4.replace("\r","") ;
                    int styleid = Integer.valueOf(r4) ;
                    return styleid ;
                }
                catch (Exception ex){
                    return 0 ;
                }
            }
        }
        return 0 ;
    }

    ///remove \n \r space \t
    private String removeAllNoMeansChar(String text){
        String t1 = text.replace(" ","") ;
        t1 = t1.replace("\n","") ;
        t1 = t1.replace("\r","") ;
        t1 = t1.replace("\t","") ;
        return t1 ;
    }

    /// check script text if contains sdui declearation.
    private boolean hasSduiDeclearation(String jsText){
        String t1 = removeAllNoMeansChar(jsText) ;
        if( t1.contains("letsdui={") || t1.contains("varsdui={")){
            return true ;
        }else{
            return false ;
        }
    }

    //获取瓦片数据的具体接口 getTile /scripts/{sid}/wmts/WMTSCapabilities.xml
    @ResponseBody
    @RequestMapping(value="/scripts/{sid}/wmts/",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public ResponseEntity<byte[]> wmtsGetTiles(@PathVariable String sid,
                                               HttpServletRequest request,
                                               ModelMap model) {

        System.out.println("ScriptsController.wmtsGetTiles sid:"+sid);
        StringBuilder requestURL = new StringBuilder(request.getRequestURL().toString());
        String queryString = request.getQueryString();
        if (queryString == null) {
            System.out.println( requestURL.toString() );
        } else {
            System.out.println( requestURL.append('?').append(queryString).toString());
        }
        Enumeration<String> params = request.getParameterNames();
        HashMap<String,String> lowerParams = new HashMap<String,String>();
        while (params.hasMoreElements())
        {
            String name1 = params.nextElement();
            String value1 = request.getParameter(name1) ;
            lowerParams.put( name1.toLowerCase() , value1) ;
        }
        String requeststr = lowerParams.get("request") ;//
        String servicestr = lowerParams.get("service")  ;//
        String zstr = lowerParams.get("tilematrix")  ;//
        String ystr = lowerParams.get("tilerow")  ;//
        String xstr = lowerParams.get("tilecol")  ;//
        String dtstr = lowerParams.get("datetime")  ;//
        String utimeStr = lowerParams.get("utime") ;
        String styleId = lowerParams.get("styleid") ;
        String sduiJsonStr = lowerParams.get("sdui") ;// '','null','{}' are all treated as no sdui.

        String jsText = ScriptsGetterTool.getSharedInstance().getScriptContent(
                Integer.valueOf(sid)
        ) ;
        /// String outputText = "sdui=" + sdui + "\n\n" + jsText;
        /// final HttpHeaders headers = new HttpHeaders();
        /// headers.setContentType(MediaType.TEXT_PLAIN);
        /// return new ResponseEntity<byte[]>(outputText.getBytes() , headers, HttpStatus.OK);

        //从数据库通过pid获取产品信息
        try{
            JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
            //get render style content
            String styleText = "" ;
            if( styleId!=null && styleId!="" ){
                styleText = rdb.rdbGetStyleText( Integer.parseInt(styleId) ) ;
            }
            if( styleText==null  || styleText.equals("")  ){
                //从代码中第一个setStyle函数的return值加载styleid
                int tempstyleid = getStyleIdFromJsScript(jsText) ;
                if( tempstyleid>0 ){
                    styleText = rdb.rdbGetStyleText( tempstyleid) ;
                }
            }

            //2022-2-7
            //检查js脚本中是否有 sdui 的声明，然后在检查GET中的sdui参数，检查GET[sdui]是否为null或者空字符串或者{}空对象
            // 前面描述的GET[sdui]均是无效的sdui，不要在js代码中附加这个sdui对象，
            // 反之，如果GET[sdui]有效的化，就把sdui={...} 写在 function main函数前面 ，后续使用AST分析进行精准替换
            String jsText2 = jsText ;
            if( hasSduiDeclearation(jsText) ){
                if( sduiJsonStr.compareTo("null") != 0
                        && sduiJsonStr.compareTo("") != 0
                        && sduiJsonStr.compareTo("{}")!=0
                ){
                    String sduiJsonStr2 = "\nsdui=" + sduiJsonStr + ";\n" ;
                    jsText2 = jsText.replace("function main(" , sduiJsonStr2 + "function main(") ;
                }
            }
            int tilez = Integer.parseInt(zstr) ;
            int tiley = Integer.parseInt(ystr) ;
            int tilex = Integer.parseInt(xstr) ;

            String extraStr = "{\"datetime\":"+dtstr+"}" ;
            HBasePeHelperCppConnector cv8 = new HBasePeHelperCppConnector();
            TileComputeResult res1 = cv8.RunScriptForTileWithRenderWithExtra(
                    "com/pixelengine/HBasePixelEngineHelper",
                    jsText2,
                    styleText,
                    extraStr,
                    tilez,
                    tiley,
                    tilex
            ) ;
            if( res1.status==0 )
            {//ok
                System.out.println("Info : tile compute ok.");
                final HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.IMAGE_PNG);
                return new ResponseEntity<byte[]>(res1.binaryData, headers, HttpStatus.OK);
            }else
            {
                System.out.println("Error : script wmts bad compute.");
                final HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.TEXT_PLAIN);
                return new ResponseEntity<byte[]>( "script wmts bad compute".getBytes(), headers, HttpStatus.NOT_FOUND);
            }

        }catch (Exception ex){
            System.out.println("Error : script wmtsGetTiles exception "  + ex.getMessage() );
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            return new ResponseEntity<byte[]>( "product wmts exception".getBytes(), headers, HttpStatus.NOT_FOUND);
        }
    }



    //2022-7-3  /scripts/wmtstclog
    @ResponseBody
    @RequestMapping(value="/scripts/wmtstclog",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult getWmtsTcLog(
            String sid,
            String datetime,
            String utime,
            String styleid,
            String sdui
    ) {

        System.out.println("ScriptsController.getWmtsTcLog sid:"+sid);
        RestResult rr = new RestResult() ;
        String jsText = ScriptsGetterTool.getSharedInstance().getScriptContent(
                Integer.valueOf(sid)
        ) ;

        try{
            JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
            //get render style content
            String styleText = "" ;
            if( styleid!=null && styleid!="" ){
                styleText = rdb.rdbGetStyleText( Integer.parseInt(styleid) ) ;
            }
            if( styleText==null  || styleText.equals("")  ){
                //从代码中第一个setStyle函数的return值加载styleid
                int tempstyleid = getStyleIdFromJsScript(jsText) ;
                if( tempstyleid>0 ){
                    styleText = rdb.rdbGetStyleText( tempstyleid) ;
                }
            }

            //2022-2-7
            //检查js脚本中是否有 sdui 的声明，然后在检查GET中的sdui参数，检查GET[sdui]是否为null或者空字符串或者{}空对象
            // 前面描述的GET[sdui]均是无效的sdui，不要在js代码中附加这个sdui对象，
            // 反之，如果GET[sdui]有效的化，就把sdui={...} 写在 function main函数前面 ，后续使用AST分析进行精准替换
            String jsText2 = jsText ;
            if( hasSduiDeclearation(jsText) ){
                if( sdui.compareTo("null") != 0
                        && sdui.compareTo("") != 0
                        && sdui.compareTo("{}")!=0
                ){
                    String sduiJsonStr2 = "\nsdui=" + sdui + ";\n" ;
                    jsText2 = jsText.replace("function main(" , sduiJsonStr2 + "function main(") ;
                }
            }
            String extraStr = "{\"datetime\":"+datetime+"}" ;
            HBasePeHelperCppConnector cv8 = new HBasePeHelperCppConnector();
            TileComputeResult res1 = cv8.RunScriptForTileWithRenderWithExtra(
                    "com/pixelengine/HBasePixelEngineHelper",
                    jsText2,
                    styleText,
                    extraStr,
                    0,
                    0,
                    0
            ) ;
            if( res1.status==0 )
            {//ok
                System.out.println("Info : tile compute ok.");
                rr.setState(0);
                rr.setMessage(res1.log);
                return rr;
            }else
            {
                rr.setState(1);
                rr.setMessage(res1.log);
                return rr ;
            }

        }catch (Exception ex){
            rr.setMessage(ex.getMessage());
            rr.setState(11);
            return rr;
        }
    }



    //2022-5-15
    @ResponseBody
    @RequestMapping(value="/scripts/update",method= RequestMethod.POST)
    @CrossOrigin(origins = "*")
    public RestResult update(
            String sid,
            String text,
            String title)
    {
        RestResult rr = new RestResult() ;

        System.out.println("/scripts/update");
        int sid2 = Integer.parseInt(sid);
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        JScript sc = rdb.rdbGetScript(sid2) ;
        if( sc == null ){
            rr.setState(1);
            rr.setMessage("No script in db for "+sid);
            return rr ;
        }

        String filename = WConfig.getSharedInstance().pedir + sc.jsfile ;
        boolean wok = FileDirTool.writeToTextFile(filename, text) ;
        if( wok==false ) {
            rr.setState(2);
            rr.setMessage("write script failed.");
            return rr ;
        }
        long dt = rdb.rdbUpdateUserScript(sid2,title) ;
        ScriptsGetterTool.getSharedInstance().updateOneScriptCache(sid2);
        rr.setState(0);
        rr.setData(dt);
        return rr;
    }


    //2022-5-15 强制刷新全部脚本
    @ResponseBody
    @RequestMapping(value="/scripts/refreshcache",method= RequestMethod.POST)
    @CrossOrigin(origins = "*")
    public RestResult refreshCache(
            String uname,
            String pwd )
    {
        RestResult rr = new RestResult() ;
        System.out.println("/scripts/refreshcache");
        ScriptsGetterTool.getSharedInstance().updateAllScriptCache();
        rr.setState(0);
        rr.setData("succ");
        return rr;
    }


    //2022-5-19 获取脚本内容，避免使用nginx缓存的影响
    @ResponseBody
    @RequestMapping(value="/scripts/content/",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult getContent(
            String sid ,
            String utime  //no used in code , only for avoid browser cache.
    )
    {
        RestResult rr = new RestResult() ;
        System.out.println("/scripts/content/");
        String sc = ScriptsGetterTool.getSharedInstance().getScriptContent(Integer.valueOf(sid));
        rr.setState(0);
        rr.setData(sc);
        return rr;
    }


    //2022-5-15 删除脚本
    @ResponseBody
    @RequestMapping(value="/scripts/delete",method= RequestMethod.POST)
    @CrossOrigin(origins = "*")
    public RestResult delete(
            String sid )
    {
        RestResult rr = new RestResult() ;
        System.out.println("/scripts/delete");
        int sid2 = Integer.parseInt(sid);
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        rdb.rdbDeleteUserScript(sid2) ;
        rr.setState(0);
        rr.setData("succ");
        return rr;
    }



    //2022-5-19
    // /pe/scripts/pixvals/...
    @ResponseBody
    @RequestMapping(value="/scripts/pixvals/",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult getPixelValues(
            String sid,
            String lon,
            String lat,
            String datetime,
            String sdui,
            String utime )
    {
        System.out.println(String.format("getPixelValues sid %s, lon %s, lat %s, dt %s,sdui %s",
                sid,lon,lat,
                datetime,sdui));
        RestResult result = new RestResult() ;
        result.setMessage("");
        result.setState(0);

        double lon1 = Double.parseDouble(lon);
        double lat1 = Double.parseDouble(lat);

        int sid2 = Integer.parseInt(sid) ;
        //get script text
        String jsText = ScriptsGetterTool.getSharedInstance().getScriptContent(sid2) ;

        //检查js脚本中是否有 sdui 的声明，然后在检查GET中的sdui参数，检查GET[sdui]是否为null或者空字符串或者{}空对象
        String jsText2 = jsText ;
        if( hasSduiDeclearation(jsText)  ){
            if( sdui.compareTo("null") != 0
                    && sdui.compareTo("") != 0
                    && sdui.compareTo("{}")!=0
            ){
                String sduiJsonStr2 = "\nsdui=" + sdui + ";\n" ;
                jsText2 = jsText.replace("function main(" , sduiJsonStr2 + "function main(") ;
            }
        }

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        try{
            //get max zoom
            HBasePeHelperCppConnector cv8 = new HBasePeHelperCppConnector();
            String dsdtJsonStr = cv8.GetDatasetNameArray("com/pixelengine/HBasePixelEngineHelper",jsText2) ;
            Gson gson = new Gson() ;
            JDsNameArrayResult dsnameArrRes = gson.fromJson(dsdtJsonStr, JDsNameArrayResult.class);
            if( dsnameArrRes==null ){
                result.setState(11);
                result.setMessage("get dsname from script failed (11).");
                return result ;
            }
            if( dsnameArrRes.status!=0 ){
                result.setState(12);
                result.setMessage("get dsname from script failed (12).");
                return result ;
            }
            if( dsnameArrRes.data.length == 0 ){
                result.setState(13);
                result.setMessage("get none dsname from script.");
                return result ;
            }
            int minOfMaxZooms = 999;
            for(int i = 0 ; i< dsnameArrRes.data.length;++i ){
                JProduct pdt1 = rdb.rdbGetProductInfoByName(dsnameArrRes.data[i]) ;
                if( pdt1==null ){
                    result.setState(14);
                    result.setMessage("get product info failed for pdtname:"+dsnameArrRes.data[i]);
                    return result ;
                }
                if( pdt1.maxZoom < minOfMaxZooms) minOfMaxZooms = pdt1.maxZoom ;
            }

            if( lon1 < -180 || lon1 > 180 || lat1 < -90 || lat1 > 90){
                ArrayList<String> arr = new ArrayList<>() ;
                arr.add("Invalid longitude or latitude.") ;
                result.setData(arr);
                return result ;
            }

            JPixelValues pxvalues = JPixelValues.CreateByLongLat(
                    lon1,
                    lat1,
                    minOfMaxZooms,
                    256,
                    256
            ) ;

            String extraStr = "{\"datetime\":"+datetime+"}" ;
            TileComputeResult res1 = cv8.RunScriptForTileWithoutRenderWithExtra(
                    "com/pixelengine/HBasePixelEngineHelper",
                    jsText2,
                    extraStr,
                    pxvalues.tilez,
                    pxvalues.tiley,
                    pxvalues.tilex
            ) ;

            if( res1.status==0 )
            {//ok
                System.out.println("Info : tile compute ok.");

                ArrayList<String> arr = new ArrayList<>() ;
                for(int ib = 0; ib < res1.nbands; ++ ib )
                {
                    double val1 = res1.getValue(pxvalues.col,pxvalues.row,ib) ;
                    arr.add("波段"+String.valueOf(ib+1) + ":" + String.valueOf(val1)) ;
                }
                result.setData(arr);
                return result ;
            }else
            {
                result.setState(0);
                ArrayList<String> tarr = new ArrayList<>();
                tarr.add("计算失败，可能像素不在有效范围内") ;
                result.setData(tarr);
                return result ;
            }
        }catch (Exception ex){
            result.setState(1);
            result.setMessage("some exception:"+ex.getMessage());
            return result ;
        }
    }


    //2022-7-17
    // /pe/scripts/pgminfo/?sid=123&datetime=20220717000000
    @ResponseBody
    @RequestMapping(value="/scripts/pgminfo/",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult getPgmInfo(
            String sid,
            String datetime
    )
    {
        System.out.println( "getPgmInfo ");
        RestResult result = new RestResult() ;
        result.setMessage("");
        result.setState(0);
        int sid2 = Integer.parseInt(sid) ;
        //get script text
        String jsText = ScriptsGetterTool.getSharedInstance().getScriptContent(sid2) ;
        if( jsText==null || jsText.compareTo("")==0){
            result.setState(90);
            result.setMessage("empty script");
            return result ;
        }
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        try{
            //get max zoom
            HBasePeHelperCppConnector cv8 = new HBasePeHelperCppConnector();

            String extraStr = "{\"datetime\":"+datetime+"}" ;
            String resText = cv8.RunScriptForTextResultWithExtra(
                    "com/pixelengine/HBasePixelEngineHelper",
                    jsText,
                    extraStr
            ) ;

            if( resText==null ){
                result.setState(11);
                result.setMessage("null result text");
            }
            else if( resText.equals("") || resText.equals("null")  ){
                result.setState(12);
                result.setMessage("empty result text");
            }else if (  resText.equals("not_string")){
                result.setState(13);
                result.setMessage("no string result");
            }else {
                result.setState(0);
                result.setMessage("");
                result.setData( resText );
            }
            return result ;
        }catch (Exception ex){
            result.setState(1);
            result.setMessage("some exception:"+ex.getMessage());
            return result ;
        }
    }

}
