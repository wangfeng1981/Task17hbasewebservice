package com.pixelengine.controller;


import com.pixelengine.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import sun.applet.Main;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
public class OnlineTaskController {
    @ResponseBody
    @RequestMapping(value="/onlinetask/new",method= RequestMethod.POST)
    @CrossOrigin(origins = "*")
    public ResponseEntity<byte[]> onlineTaskNew(String script, String userid) {
        int uid = Integer.parseInt(userid) ;

        //run for style
        HBasePeHelperCppConnector cv8 = new HBasePeHelperCppConnector();
        String errorText = cv8.CheckScriptOk( "com/pixelengine/HBasePixelEngineHelper", script) ;
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if( errorText.compareTo("")!=0 )
        {
            System.out.println("Error : CheckScriptOk bad , " + errorText);

            String outjson = "{\"oltid\":-1,\"message\":\"" + errorText+"\"}" ;
            return new ResponseEntity<byte[]>( outjson.getBytes(), headers, HttpStatus.OK);
        }

        String styleresult = cv8.RunToGetStyleFromScript("com/pixelengine/HBasePixelEngineHelper", script ) ;
        if( styleresult.compareTo("")==0 )
        {
            System.out.println("Info : get a emtpy style string.");
        }

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        int newoltid= rdb.rdbNewRenderTask(script,styleresult,uid) ;
        String outjson = "{\"oltid\":"+newoltid+",\"message\":\"\"}" ;
        return new ResponseEntity<byte[]>( outjson.getBytes(), headers, HttpStatus.OK);
    }

    //online task id - otid
    @ResponseBody
    @RequestMapping(value="/onlinetask/wmts/{otid}/WMTSCapabilities.xml",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public ResponseEntity<byte[]> wmtsGetCap(@PathVariable String otid) throws IOException {
        System.out.println("/onlinetask/wmts/{otid}/WMTSCapabilities.xml");
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);

        //read template
        String xmlfile = WConfig.sharedConfig.wmtsxml;

        //Resource resource = new ClassPathResource("resources:wmts-template.xml");
        java.io.InputStream instream0 = new FileInputStream(xmlfile);
        InputStreamReader  reader0 = new InputStreamReader(instream0, "UTF-8");
        BufferedReader bf = new BufferedReader(reader0);
        String xmlContent = "";
        String newLine = "";
        while((newLine = bf.readLine()) != null){
            xmlContent += newLine ;
        }

        //replace {oltid} with real online task id.
        String xmlContent2 = xmlContent.replace("{oltid}",otid);

        //return xml
        return new ResponseEntity<byte[]>(xmlContent2.getBytes(), headers, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value="/onlinetask/wmts/{otid}/",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public ResponseEntity<byte[]> wmtsGetTiles(@PathVariable String otid, HttpServletRequest request, ModelMap model) {

        System.out.println("wmtsGetTiles");
        System.out.println("onlinetaskid:"+otid);

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
            System.out.println( name1 + ":" + value1    );
            lowerParams.put( name1.toLowerCase() , value1) ;
        }

        String requeststr = lowerParams.get("request") ;//
        String servicestr = lowerParams.get("service")  ;//req.get_param_value("SERVICE");
        String zstr = lowerParams.get("tilematrix")  ;//req.get_param_value("TILEMATRIX");
        String ystr = lowerParams.get("tilerow")  ;// req.get_param_value("TILEROW");
        String xstr = lowerParams.get("tilecol")  ;// req.get_param_value("TILECOL");
        String dtstr = lowerParams.get("dt")  ;//req.get_param_value("dt");
        System.out.println("request:"+request) ;
        System.out.println("service:" + servicestr ) ;
        System.out.println("tilematrix:" + zstr) ;
        System.out.println("tilerow:" + ystr) ;
        System.out.println("tilecol:" + xstr) ;
        System.out.println("dt:" + dtstr ) ;

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        JRenderTask renderTask = rdb.rdbGetRenderTask( Integer.parseInt(otid) ) ;
        if( renderTask != null )
        {
            HBasePeHelperCppConnector cv8 = new HBasePeHelperCppConnector();
            String stylejson = renderTask.renderStyle;
            if( stylejson==null ){
                stylejson="";
            }
            TileComputeResult res1 = cv8.RunScriptForTileWithRender(
                    "com/pixelengine/HBasePixelEngineHelper",
                    renderTask.scriptContent,
                    stylejson,
                    Long.parseLong(dtstr),
                    Integer.parseInt(zstr),
                    Integer.parseInt(ystr),
                    Integer.parseInt(xstr)) ;
            if( res1.status==0 )
            {//ok
                System.out.println("Info : tile compute ok.");
                final HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.IMAGE_PNG);
                //return new ResponseEntity<byte[]>(retpng, headers, HttpStatus.OK);
                return new ResponseEntity<byte[]>(res1.binaryData, headers, HttpStatus.OK);
            }else
            {
                System.out.println("Error : bad compute.");
                final HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.TEXT_PLAIN);
                return new ResponseEntity<byte[]>( "bad compute".getBytes(), headers, HttpStatus.NOT_FOUND);
            }
        }else
        {
            System.out.println("Error : not find render task.");
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            return new ResponseEntity<byte[]>( "not found render task".getBytes(), headers, HttpStatus.NOT_FOUND);
        }
    }



//    svr.Get(R"(/pe/onlinetask/wmts/(\d+)/WMTSCapabilities.xml)", handle_onlinetask_wmts_capabilities);
//    svr.Get(R"(/pe/onlinetask/wmts/(\d+)/)", handle_onlinetask_wmts_gettiles);// \d+ is online task id = oltid
}
