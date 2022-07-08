package com.pixelengine.controller;
//实现系统预定义产品的wmts服务
//updated 2022-4-17
//updated 2022-4-25 use task17_api_root
//2022-5-19
//2022-7-8

import com.pixelengine.*;
import com.pixelengine.DataModel.*;
import com.pixelengine.TileComputeResult;
import com.pixelengine.tools.FileDirTool;
import com.pixelengine.tools.ScriptsGetterTool;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

@RestController
public class ProductWMTSController {
    //系统产品标准渲染Script Template
    private String scriptContentTemplate = "function main(){"
            +"var ds=pe.Dataset('{{{name}}}', {{{dt}}} );"
            +"return ds; } " ;
    private String scriptContentWithRoiTemplate = "function main(){"
            +"var ds=pe.Dataset('{{{name}}}', {{{dt}}} );"
            +"return ds.clip2('{{{roiid}}}',{{{nodata}}}); } " ;

    // /pe/product/123/wmts/...
    // /pe/uproduct/123/wmts/...
    @ResponseBody
    @RequestMapping(value="/{product}/{pid}/wmts/WMTSCapabilities.xml",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public ResponseEntity<byte[]> wmtsGetCap(
            @PathVariable String pid,
            @PathVariable String product

    ) throws IOException {
        System.out.println("ProductWMTSController.wmtsGetCap");
        System.out.println("/" +product + "/"+pid.toString()+"/wmts/WMTSCapabilities.xml");

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        //从数据库查询zlevel数值 数据库中tbProduct.maxZoom write to zlevel.
        JProduct pdt  = rdb.rdbGetProductForAPI(Integer.parseInt(pid));
        Long currentDateTime = JRDBHelperForWebservice.sgetCurrentDatetime();
        JProductDataItem dataItem = rdb.rdbGetLowerEqualNearestDt0(pdt.pid,currentDateTime,pdt.timeType);

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);

        //read template
        String xmlfile = WConfig.getSharedInstance().productwmts ;
        //Resource resource = new ClassPathResource("resources:productwmts-template.xml");
        java.io.InputStream instream0 = new FileInputStream(xmlfile);
        InputStreamReader  reader0 = new InputStreamReader(instream0, "UTF-8");
        BufferedReader bf = new BufferedReader(reader0);
        String xmlContent = "";
        String newLine = "";
        while((newLine = bf.readLine()) != null){
            xmlContent += newLine ;
        }
        //replace {oltid} with real online task id.
        String xmlContent2 = xmlContent.replace("{pid}",pid);

        //replace ms_{zlevel} with real zlevel.
        xmlContent2 = xmlContent2.replace("{maxZoom}", String.valueOf(pdt.maxZoom) );
        //host
        xmlContent2 = xmlContent2.replace("{host}", WConfig.getSharedInstance().host );
        //port
        xmlContent2 = xmlContent2.replace("{port}", WConfig.getSharedInstance().port);
        //datetime
        xmlContent2 = xmlContent2.replace("{datetime}", String.valueOf(dataItem.hcol) );
        //styleid
        xmlContent2 = xmlContent2.replace("{styleid}", String.valueOf(pdt.styleid) );

        //task17_api_root
        xmlContent2 = xmlContent2.replace("{task17_api_root}", WConfig.getSharedInstance().task17_api_root );


        //return xml
        return new ResponseEntity<byte[]>(xmlContent2.getBytes(), headers, HttpStatus.OK);
    }




    //获取瓦片数据的具体接口 getTile /product/{pid}/wmts/WMTSCapabilities.xml
    @ResponseBody
    @RequestMapping(value="/product/{pid}/wmts/",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public ResponseEntity<byte[]> wmtsGetTiles(@PathVariable String pid,
                                               HttpServletRequest request,
                                               ModelMap model) {

        System.out.println("ProductWMTSController.wmtsGetTiles");
        System.out.println("pid:"+pid);
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
            //System.out.println( name1 + ":" + value1    );
            lowerParams.put( name1.toLowerCase() , value1) ;
        }
        String requeststr = lowerParams.get("request") ;//
        String servicestr = lowerParams.get("service")  ;//req.get_param_value("SERVICE");
        String zstr = lowerParams.get("tilematrix")  ;//req.get_param_value("TILEMATRIX");
        String ystr = lowerParams.get("tilerow")  ;// req.get_param_value("TILEROW");
        String xstr = lowerParams.get("tilecol")  ;// req.get_param_value("TILECOL");
        String dtstr = lowerParams.get("datetime")  ;//req.get_param_value("dt");
        String styleId = lowerParams.get("styleid") ;
        String roiid = lowerParams.get("roiid") ; // optional , null , "null" , "" are treated as no roi; good value exmaple are user:123 or sys:456.


//        System.out.println("request:"+request) ;
//        System.out.println("service:" + servicestr ) ;
//        System.out.println("tilematrix:" + zstr) ;
//        System.out.println("tilerow:" + ystr) ;
//        System.out.println("tilecol:" + xstr) ;
//        System.out.println("datetime:" + dtstr ) ;
//        System.out.println("styleid:" + styleId) ;


        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        //从数据库通过pid获取产品信息
        try{
            JProduct pdt = rdb.rdbGetProductForAPI( Integer.parseInt(pid)  ) ;

            //get render style content
            String pdtStyle = "" ;
            if( styleId==null  || styleId.equals("") || styleId.equals("default") ){
                pdtStyle = rdb.rdbGetStyleText( pdt.styleid) ;
            }else{
                pdtStyle = rdb.rdbGetStyleText( Integer.parseInt(styleId) ) ;
            }
            System.out.println("style ok") ;

            long dtlongvalue = 0 ;
            try{
                dtlongvalue = Long.parseLong(dtstr) ;
            }catch (Exception ex){
                //bad long
                dtlongvalue = 0 ;
            }

            boolean useRoiClip = true ;
            if( roiid==null || roiid.equals("") || roiid.equals("null") ){
                useRoiClip = false ;
            }

            if( pdt.name.equals("") == false )
            {
                HBasePeHelperCppConnector cv8 = new HBasePeHelperCppConnector();
                String scriptContent = scriptContentTemplate.replace("{{{name}}}", pdt.name) ;
                if( useRoiClip==true ){//2022-4-17
                    scriptContent=scriptContentWithRoiTemplate.replace("{{{name}}}",pdt.name);
                    scriptContent=scriptContent.replace("{{{roiid}}}", roiid);
                    String nodataStr = String.valueOf( pdt.bandList.get(0).noData ) ;
                    if( nodataStr.equals("") )nodataStr="0";
                    scriptContent=scriptContent.replace("{{{nodata}}}", nodataStr);
                }

                scriptContent = scriptContent.replace("{{{dt}}}" , Long.toString(dtlongvalue) ) ;
                TileComputeResult res1 = cv8.RunScriptForTileWithRenderWithExtra(
                        "com/pixelengine/HBasePixelEngineHelper",
                        scriptContent,
                        pdtStyle,
                        "{\"datetime\":"+dtstr+"}", //2022-7-3
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
                System.out.println("Error : product no find ");
                final HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.TEXT_PLAIN);
                return new ResponseEntity<byte[]>( "not find product".getBytes(), headers, HttpStatus.NOT_FOUND);
            }
        }catch (Exception ex){
            System.out.println("Error : product wmts exception .");
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            return new ResponseEntity<byte[]>( "product wmts exception".getBytes(), headers, HttpStatus.NOT_FOUND);
        }

    }



    //获取0,0,0瓦片计算的log 信息 2022-7-3
    @ResponseBody
    @RequestMapping(value="/product/wmtstclog",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult getTcLog(String pid,String datetime, String styleid, String roiid) {
        RestResult rr = new RestResult() ;

        System.out.println("ProductWMTSController.wmtstclog");
        System.out.println("pid:"+pid);

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        //从数据库通过pid获取产品信息
        try{
            JProduct pdt = rdb.rdbGetProductForAPI( Integer.parseInt(pid)  ) ;

            //get render style content
            String pdtStyle = "" ;
            if( styleid==null  || styleid.equals("") || styleid.equals("default") ){
                pdtStyle = rdb.rdbGetStyleText( pdt.styleid) ;
            }else{
                pdtStyle = rdb.rdbGetStyleText( Integer.parseInt(styleid) ) ;
            }
            System.out.println("style ok") ;

            long dtlongvalue = 0 ;
            try{
                dtlongvalue = Long.parseLong(datetime) ;
            }catch (Exception ex){
                //bad long
                dtlongvalue = 0 ;
            }

            boolean useRoiClip = true ;
            if( roiid==null || roiid.equals("") || roiid.equals("null") ){
                useRoiClip = false ;
            }

            if( pdt.name.equals("") == false )
            {
                HBasePeHelperCppConnector cv8 = new HBasePeHelperCppConnector();
                String scriptContent = scriptContentTemplate.replace("{{{name}}}", pdt.name) ;
                if( useRoiClip==true ){//2022-4-17
                    scriptContent=scriptContentWithRoiTemplate.replace("{{{name}}}",pdt.name);
                    scriptContent=scriptContent.replace("{{{roiid}}}", roiid);
                    String nodataStr = String.valueOf( pdt.bandList.get(0).noData ) ;
                    if( nodataStr.equals("") )nodataStr="0";
                    scriptContent=scriptContent.replace("{{{nodata}}}", nodataStr);
                }
                scriptContent = scriptContent.replace("{{{dt}}}" , datetime ) ;
                TileComputeResult res1 = cv8.RunScriptForTileWithRenderWithExtra(
                        "com/pixelengine/HBasePixelEngineHelper",
                        scriptContent,
                        pdtStyle,
                        "{\"datetime\":"+datetime+"}",
                        0,
                        0,
                        0) ;
                if( res1.status==0 )
                {//ok
                    System.out.println("Info : tile compute ok.");
                    rr.setState(0);
                    rr.setMessage(res1.log);
                    return rr ;
                }else
                {
                    rr.setState(1);
                    rr.setMessage(res1.log);
                    return rr ;
                }
            }else
            {
                rr.setState(11);
                rr.setMessage("invalid pid.");
                return rr ;
            }
        }catch (Exception ex){
            rr.setState(12);
            rr.setMessage(ex.getMessage());
            return rr ;
        }
    }


    // /pe/product/123/pixvals/...
    // /pe/uproduct/123/pixvals/...
    @ResponseBody
    @RequestMapping(value="/product/pixvals/",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult getPixelValues(
            String pid,
            String lon,
            String lat,
            String datetime )
    {
        System.out.println(String.format("getPixelValues lon %s, lat %s, dt %s",lon,lat,datetime));
        RestResult result = new RestResult() ;
        result.setMessage("");
        result.setState(0);

        double lon1 = Double.parseDouble(lon);
        double lat1 = Double.parseDouble(lat);

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        try{
            JProduct pdt = rdb.rdbGetProductForAPI( Integer.parseInt(pid)  ) ;
            if( pdt==null ){
                ArrayList<String> arr = new ArrayList<>() ;
                arr.add("Invalid pid.") ;
                result.setData(arr);
                return result ;
            }

            if( lon1 < -180 || lon1 > 180 || lat1 < -90 || lat1 > 90){
                ArrayList<String> arr = new ArrayList<>() ;
                arr.add("Invalid longitude or latitude.") ;
                result.setData(arr);
                return result ;
            }

            int tilez = pdt.maxZoom;
            JPixelValues pxvalues = JPixelValues.CreateByLongLat(
                    lon1,
                    lat1,
                    tilez,
                    pdt.tileWid,
                    pdt.tileHei
            ) ;

            String scriptContent = scriptContentTemplate.replace("{{{name}}}",pdt.name)
                    .replace("{{{dt}}}" , datetime) ;
            HBasePeHelperCppConnector cv8 = new HBasePeHelperCppConnector();
            TileComputeResult res1 = cv8.RunScriptForTileWithoutRender(
                    "com/pixelengine/HBasePixelEngineHelper",
                    scriptContent, Long.parseLong(datetime) ,
                    tilez,pxvalues.tiley,pxvalues.tilex) ;
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
                result.setState(3);
                result.setMessage("bad compute in v8.");
                return result ;
            }
        }catch (Exception ex){
            result.setState(1);
            result.setMessage("some exception:"+ex.getMessage());
            return result ;
        }
    }




}
