package com.pixelengine.controller;

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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Optional;

@RestController
public class ScriptsController {

    @ResponseBody
    @RequestMapping(value="/scripts/user/new/{uid}",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public ResponseEntity<byte[]> scriptnew(@PathVariable String uid , String type)
    {
        System.out.println("/scripts/user/new/{uid}");
        int uid2 = Integer.parseInt(uid) ;
        String scriptZero="";
        if( type.compareTo("1")==0 )
        {
            scriptZero = "function main(){\n  return null;\n}" ;
        }else
        {
            scriptZero = "function zlevelFunc()\n{\n  return 1;\n}\nfunction extentFunc()\n{\n  return [110.0,120.0,35.0,32.0];//left,right,top,bottom\n}\nfunction sharedobjectFunc()\n{\n  return {};\n}\nfunction mapFunc( sharedobj )\n{\n  return {key:\"somekey\", val:{data:1}} ;\n}\nfunction reduceFunc( sharedObj, key, obj1, obj2 )\n{\n  var sum=obj1.data+obj2.data;\n  return {data:sum};\n}\nfunction main( objCollection )\n{\n  var key0sum= objCollection[0].data;\n  return {data:key0sum};\n}";
        }
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        int newsid = rdb.rdbNewUserScript(uid2,scriptZero, Integer.parseInt(type)) ;
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String outjson = "{\"sid\":" + newsid + ", \"type\":" +type+ "}" ;
        return new ResponseEntity<byte[]>( outjson.getBytes(), headers, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value="/scripts/user/{uid}",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public ResponseEntity<byte[]> scriptlist(@PathVariable String uid)
    {
        System.out.println("/scripts/user/{uid}");
        int uid2 = Integer.parseInt(uid);

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        String outjson = rdb.rdbGetUserScriptListJson(uid2) ;

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<byte[]>( outjson.getBytes(), headers, HttpStatus.OK);
    }


    //获取js脚本中的内容 relPath 是相对路径 scrips/...
    private String getScriptContent( String relPath )
    {
        String fullpath = WConfig.getSharedInstance().pedir + relPath ;
        String data = "";
        try{
            data = new String(Files.readAllBytes(Paths.get(fullpath)));
        }catch (Exception ex){
            System.out.println("getScriptContent exception:"+ex.getMessage());
        }
        return data;
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
                xmlContent2 = xmlContent2.replace("{utime}",  String.valueOf(scriptObj.utime.getTime()) ) ;
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
        String sduiJsonStr = lowerParams.get("sdui") ;

        String jsText = ScriptsGetterTool.getSharedInstance().getScriptContent( Integer.valueOf(sid) , Long.valueOf(utimeStr)) ;
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

            //2022-2-7
            //检查js脚本中是否有 sdui 的声明，然后在检查GET中的sdui参数，检查GET[sdui]是否为null或者空字符串或者{}空对象
            // 前面描述的GET[sdui]均是无效的sdui，不要在js代码中附加这个sdui对象，
            // 反之，如果GET[sdui]有效的化，就把sdui={...} 写在 function main函数前面 ，后续使用AST分析进行精准替换
            String jsText2 = jsText ;
            if( jsText.contains("sdui={") == true ){
                if( sduiJsonStr.compareTo("null") != 0 && sduiJsonStr.compareTo("") != 0 && sduiJsonStr.compareTo("{}")!=0 ){
                    String sduiJsonStr2 = "\nsdui=" + sduiJsonStr + ";\n" ;
                    jsText2 = jsText.replace("function main(" , sduiJsonStr2 + "function main(") ;
                }
            }
            int tilez = Integer.parseInt(zstr) ;
            int tiley = Integer.parseInt(ystr) ;
            int tilex = Integer.parseInt(xstr) ;

            if( tilez==0 && tiley==0 && tilex==0 ){
                System.out.println("debug at 0,0,0 scriptWithSDUI:");
                System.out.println(jsText2);
            }

            if( false ){//debug
                final HttpHeaders debugheaders = new HttpHeaders();
                debugheaders.setContentType(MediaType.TEXT_PLAIN);
                return new ResponseEntity<byte[]>(jsText2.getBytes() , debugheaders, HttpStatus.OK);
            }

            
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

    @ResponseBody
    @RequestMapping(value="/scripts/update/{sid}",method= RequestMethod.POST)
    @CrossOrigin(origins = "*")
    public ResponseEntity<byte[]> scriptupdate(
            @PathVariable String sid,
            @RequestParam("script") Optional<String> script,
            @RequestParam("title") Optional<String> title)
    {
        System.out.println("/scripts/update/{sid}");
        int sid2 = Integer.parseInt(sid);
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        if( script.isPresent() && title.isPresent() )
        {
            rdb.rdbUpdateUserScript(sid2,script.get(),title.get());
        }else if( title.isPresent() )
        {
            rdb.rdbUpdateUserScript(sid2,null,title.get());
        }else if( script.isPresent() )
        {
            rdb.rdbUpdateUserScript(sid2,script.get(),null);
        }
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<byte[]>( "{\"status\":0}".getBytes(), headers, HttpStatus.OK);
    }



}
