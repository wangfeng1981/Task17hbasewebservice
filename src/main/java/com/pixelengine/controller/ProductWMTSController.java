package com.pixelengine.controller;
//实现系统预定义产品的wmts服务
import com.pixelengine.*;
import com.pixelengine.DataModel.JProduct;
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
    @ResponseBody
    @RequestMapping(value="/product/{pid}/wmts/WMTSCapabilities.xml",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public ResponseEntity<byte[]> wmtsGetCap(@PathVariable String pid) throws IOException {
        System.out.println("ProductWMTSController.wmtsGetCap");
        System.out.println("/product/"+pid.toString()+"/wmts/WMTSCapabilities.xml");

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        //从数据库查询zlevel数值 后面这个地方要修改，数据库中tbProduct.maxZoom write to zlevel.
        JProduct pdt  = new JProduct() ; pdt.maxZoom = 12 ;




        //read template
        String xmlfile = WConfig.sharedConfig.productwmts ;
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
        xmlContent2 = xmlContent2.replace("{host}", WConfig.sharedConfig.host );
        //port
        xmlContent2 = xmlContent2.replace("{port}", WConfig.sharedConfig.port);

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
            System.out.println( name1 + ":" + value1    );
            lowerParams.put( name1.toLowerCase() , value1) ;
        }
        String requeststr = lowerParams.get("request") ;//
        String servicestr = lowerParams.get("service")  ;//req.get_param_value("SERVICE");
        String zstr = lowerParams.get("tilematrix")  ;//req.get_param_value("TILEMATRIX");
        String ystr = lowerParams.get("tilerow")  ;// req.get_param_value("TILEROW");
        String xstr = lowerParams.get("tilecol")  ;// req.get_param_value("TILECOL");
        String dtstr = lowerParams.get("datetime")  ;//req.get_param_value("dt");
        String styleId = lowerParams.get("style") ;//default or styleid

        System.out.println("request:"+request) ;
        System.out.println("service:" + servicestr ) ;
        System.out.println("tilematrix:" + zstr) ;
        System.out.println("tilerow:" + ystr) ;
        System.out.println("tilecol:" + xstr) ;
        System.out.println("datetime:" + dtstr ) ;
        System.out.println("style:" + styleId) ;

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
//        JRenderTask renderTask = rdb.rdbGetRenderTask( Integer.parseInt(otid) ) ;


        //从数据库通过pid获取产品信息
        JProduct pdt = new JProduct() ; pdt.name = "geewater" ; pdt.styleid=2 ;

        //get render style content
        String pdtStyle = rdb.rdbGetStyleText( pdt.styleid) ;


        if( pdt.name.compareTo("") != 0 )
        {
            HBasePeHelperCppConnector cv8 = new HBasePeHelperCppConnector();
            String scriptContent = "function main(){"
                    +"var ds=pe.Dataset('"+pdt.name+"', "+dtstr+" );" //this need change v8 interface, if no bands pass in then return all bands data.
                    +"return ds; } " ;
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
            System.out.println("Error : not find render task.");
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            return new ResponseEntity<byte[]>( "not found render task".getBytes(), headers, HttpStatus.NOT_FOUND);
        }
    }

}
