package com.pixelengine.controller;


import com.google.gson.Gson;
import com.pixelengine.*;
import com.pixelengine.DataModel.JProduct;
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
    //这个是老接口，后面不再更新，请使用新的接口 /rendertask/wmts/{otid}/WMTSCapabilities.xml 2021-1-28
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

    //这个是老的版本，使用currenttime的，新的使用extraData对象,请使用新的接口 /rendertak/wmts/{otid}/ 2021-1-28
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


    @ResponseBody
    @RequestMapping(value="/onlinetask/getpixel/",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public ResponseEntity<byte[]> onlineTaskGetPixelValues(
            String oltid, String longitude,String latitude,String dt )
    {

        System.out.println("onlinetask getpixel values");
        System.out.println("onlinetaskid,longitude,latitude,dt:"+oltid+","+longitude+","+latitude+","+dt);

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        JRenderTask renderTask = rdb.rdbGetRenderTask( Integer.parseInt(oltid) ) ;

        double inlon = Double.parseDouble(longitude);
        double inlat = Double.parseDouble(latitude);

        if( renderTask != null && inlon>=-180.0 && inlon<=180.0 && inlat>=-90.0 && inlat<=90.0 )
        {

            HBasePeHelperCppConnector cv8 = new HBasePeHelperCppConnector();

            //解析脚本，获取一个产品名称，从而计算最大zlevel
            String dsdtJsonText = cv8.ParseScriptForDsDt(
                    "com/pixelengine/HBasePixelEngineHelper",
                    renderTask.scriptContent);

            Gson gson = new Gson();
            JDsDtResult dsdtResult = gson.fromJson(dsdtJsonText, JDsDtResult.class) ;
            if( dsdtResult.status==0 )
            {//good

                //get product info by ds name
                String dsname1 = dsdtResult.dsdtarr[0].ds;
                JProduct pdt = rdb.rdbGetProductInfoByName(dsname1) ;
                if( pdt != null )
                {
                    int tilez = pdt.maxZoom;
                    JPixelValues pxvalues = JPixelValues.CreateByLongLat(
                            Double.parseDouble(longitude),
                            Double.parseDouble(latitude),
                            tilez,
                            pdt.tileWid,
                            pdt.tileHei
                    ) ;

                    System.out.println("maxzoom:" + tilez);
                    System.out.println("from long,lat -> tilez,y,x:"
                            +pxvalues.tilez
                            +","+pxvalues.tiley
                            +","+pxvalues.tilex) ;
                    System.out.println("col,row:" + pxvalues.col + "," + pxvalues.row) ;
                    TileComputeResult res1 = cv8.RunScriptForTileWithoutRender(
                            "com/pixelengine/HBasePixelEngineHelper",
                            renderTask.scriptContent, Long.parseLong(dt) ,
                            tilez,pxvalues.tiley,pxvalues.tilex) ;
                    if( res1.status==0 )
                    {//ok
                        System.out.println("Info : tile compute ok.");
                        final HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        pxvalues.values = new double[res1.nbands] ;

                        for(int ib = 0; ib < res1.nbands; ++ ib )
                        {
                            pxvalues.values[ib] = res1.getValue(pxvalues.col,pxvalues.row,ib) ;
                        }
                        String jsontext = gson.toJson(pxvalues, JPixelValues.class);
                        return new ResponseEntity<byte[]>(jsontext.getBytes(), headers, HttpStatus.OK);
                    }else
                    {
                        System.out.println("Error : bad compute.");
                        final HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.TEXT_PLAIN);
                        return new ResponseEntity<byte[]>( "bad compute".getBytes(), headers, HttpStatus.NOT_FOUND);
                    }
                }else
                {
                    System.out.println("Error : no productinfo, "+ dsname1);
                    final HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.TEXT_PLAIN);
                    return new ResponseEntity<byte[]>( "no productinfo".getBytes(), headers, HttpStatus.NOT_FOUND);
                }
            }else{
                System.out.println("Error : ParseScriptForDsDt failed, "+ dsdtResult.error);
                final HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.TEXT_PLAIN);
                return new ResponseEntity<byte[]>( "ParseScriptForDsDt failed".getBytes(), headers, HttpStatus.NOT_FOUND);
            }
        }else
        {
            System.out.println("Error : not find render task or longlat invalid.");
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            return new ResponseEntity<byte[]>( "not found render task or longlat invalid.".getBytes(), headers, HttpStatus.NOT_FOUND);
        }
    }


    //online task id - otid
    //新的接口 /rendertask/wmts/{otid}/WMTSCapabilities.xml 2021-1-28
    @ResponseBody
    @RequestMapping(value="/rendertask/wmts/{otid}/WMTSCapabilities.xml",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public ResponseEntity<byte[]> wmtsGetCap2(@PathVariable String otid) throws IOException {
        System.out.println("/rendertask/wmts/{otid}/WMTSCapabilities.xml");
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);

        //从数据库查询zlevel数值
        String zlevel = "9" ;

        //read template
        String xmlfile = WConfig.sharedConfig.wmtsxml2;

        //Resource resource = new ClassPathResource("resources:wmts-template2.xml");
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

        //replace ms_{zlevel} with real zlevel.
        xmlContent2 = xmlContent2.replace("{zlevel}",zlevel);

        //return xml
        return new ResponseEntity<byte[]>(xmlContent2.getBytes(), headers, HttpStatus.OK);
    }

    //新接口 /rendertak/wmts/{otid}/ 2021-1-28
    @ResponseBody
    @RequestMapping(value="/rendertask/wmts/{otid}/",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public ResponseEntity<byte[]> wmtsGetTiles2(@PathVariable String otid, HttpServletRequest request, ModelMap model) {

        System.out.println("wmtsGetTiles2");
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
        String styleId = lowerParams.get("style") ;

        System.out.println("request:"+request) ;
        System.out.println("service:" + servicestr ) ;
        System.out.println("tilematrix:" + zstr) ;
        System.out.println("tilerow:" + ystr) ;
        System.out.println("tilecol:" + xstr) ;
        System.out.println("dt:" + dtstr ) ;
        System.out.println("style:" + styleId) ;

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        JRenderTask renderTask = rdb.rdbGetRenderTask( Integer.parseInt(otid) ) ;
        if( renderTask != null )
        {
            HBasePeHelperCppConnector cv8 = new HBasePeHelperCppConnector();

            //渲染方案
            String stylejsonText = null ;

            if( styleId.compareTo("default")==0 ){
                //get style from table of tbRenderTask
                stylejsonText = renderTask.renderStyle ;
            }else{
                int getStyleId = Integer.parseInt(styleId) ;
                stylejsonText = rdb.rdbGetStyleText(getStyleId) ;
            }
            if( stylejsonText==null ){
                stylejsonText="";
            }

            String extraText = "{\"datetime\":" + dtstr + "}" ;

            TileComputeResult res1 = cv8.RunScriptForTileWithRenderWithExtra(
                    "com/pixelengine/HBasePixelEngineHelper",
                    renderTask.scriptContent,
                    stylejsonText,
                    extraText,
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
