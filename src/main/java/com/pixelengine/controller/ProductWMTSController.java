package com.pixelengine.controller;
//实现系统预定义产品的wmts服务
//updated 2022-4-17

import com.pixelengine.*;
import com.pixelengine.DataModel.*;
import com.pixelengine.TileComputeResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
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
        JProductDataItem dataItem = rdb.rdbGetLowerEqualNearestHCol(pdt.pid,currentDateTime,pdt.timeType);

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
                        "{}",
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


        /// 读取placeholder png文件， 将xyz文本写入图片直接返回，目前先不通过hbase拿数据，不通过v8计算。
//        try{
//            InputStream instream = this.getClass().getResourceAsStream("/placeholder.png");
//            BufferedImage image = ImageIO.read(instream);
//            Graphics g = image.getGraphics();
//            g.setFont(g.getFont().deriveFont(12f));
//            g.setColor(Color.black);
//            String xyzStr = "x:"+xstr+",y:"+ystr+",z:"+zstr ;
//            g.drawString(xyzStr, 0, 20);
//            g.dispose();
//            //ImageIO.write(image, "png", new File("test.png"));
//            ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
//            ImageOutputStream imgoutput = ImageIO.createImageOutputStream(bytestream);
//            ImageIO.write(image , "png" ,imgoutput ) ;
//            final HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.IMAGE_PNG);
//            return new ResponseEntity<byte[]>(bytestream.toByteArray(), headers, HttpStatus.OK);
//        }catch (Exception ex ) {
//            String img = this.getClass().getResource("/placeholder.png").getPath();
//            System.out.println("read img:" + img);
//            System.out.println(ex.getMessage());
//            final HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.TEXT_PLAIN);
//            return new ResponseEntity<byte[]>("bad wmts placeholder image".getBytes(), headers, HttpStatus.NOT_FOUND);
//
//        }
    }


    // /pe/product/123/pixvals/...
    // /pe/uproduct/123/pixvals/...
    @ResponseBody
    @RequestMapping(value="/product/{pid}/pixvals/",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult getPixelValues(
            @PathVariable String pid,
            String lon,String lat,String datetime )
    {
        System.out.println(String.format("getPixelValues lon %s, lat %s, dt %s",lon,lat,datetime));
        RestResult result = new RestResult() ;
        result.setMessage("");
        result.setState(0);

        double inlon = Double.parseDouble(lon);
        double inlat = Double.parseDouble(lat);

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        try{
            JProduct pdt = rdb.rdbGetProductForAPI( Integer.parseInt(pid)  ) ;
            if( pdt!=null && pdt.name.equals("")==false &&
                    inlon>=-180.0 && inlon<=180.0 && inlat>=-90.0 && inlat<=90.0)
            {
                int tilez = pdt.maxZoom;
                JPixelValues pxvalues = JPixelValues.CreateByLongLat(
                        inlon,
                        inlat,
                        tilez,
                        pdt.tileWid,
                        pdt.tileHei
                ) ;
                System.out.println("from long,lat -> tile(z,y,x),col,row:"
                        +"(" +pxvalues.tilez
                        +"," +pxvalues.tiley
                        +"," +pxvalues.tilex
                        +"),"+pxvalues.col
                        + "," + pxvalues.row
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
                    pxvalues.values = new double[res1.nbands] ;
                    for(int ib = 0; ib < res1.nbands; ++ ib )
                    {
                        pxvalues.values[ib] = res1.getValue(pxvalues.col,pxvalues.row,ib) ;
                    }
                    result.setData(pxvalues);
                    return result;
                }else
                {
                    result.setState(3);
                    result.setMessage("bad compute in v8.");
                    return result ;
                }
            }else{
                result.setState(2);
                result.setMessage("no product or invalid longitude/latitude.");
                return result ;
            }
        }catch (Exception ex){
            result.setState(1);
            result.setMessage("mysql query or some other exception:"+ex.getMessage());
            return result ;
        }
    }

}
