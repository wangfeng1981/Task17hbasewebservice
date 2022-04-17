package com.pixelengine.controller;
/// 在线作图java这边相关接口，主要是mysql相关操作 2022-4-17
/// 2022-4-17 created

import com.google.gson.Gson;
import com.pixelengine.DataModel.*;
import com.pixelengine.HBasePeHelperCppConnector;
import com.pixelengine.HBasePixelEngineHelper;
import com.pixelengine.JRDBHelperForWebservice;
import com.pixelengine.tools.FileDirTool;
import com.pixelengine.tools.HttpTool;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import scala.reflect.internal.tpe.FindMembers;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

@RestController
@RequestMapping("/omc")
public class OmcController {


    public void TestPost () {
        String omcApi = WConfig.getSharedInstance().omc_localhost_api ;
        //新新建一个qgs项目
        String qgsfile = "" ;
        {
            HttpTool http = new HttpTool() ;
            if( http.omcRpc(omcApi ,  "project.new" , "{}") ==0 )
            {
                System.out.println("Result:");
                System.out.println(http.getResult());
            }else{
                System.out.println("Error:\n" + http.getError());
            }
        }
    }


    //
    @CrossOrigin(origins = "*")
    @RequestMapping("/filelist")
    @ResponseBody
    public RestResult getFileList(String uid,
                                  String type  // 1 qgs| 2 img | 3 vec
    ) {

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
        ArrayList<OmcFile> data = rdb.rdbGetOmcFileList( Integer.valueOf(uid) , Integer.valueOf(type) ) ;
        if( data==null ){
            RestResult result = new RestResult();
            result.setState(1);
            result.setMessage("rdbGetOmcFileList exception.");
            result.setData("");
            return result ;
        }else{
            RestResult result = new RestResult();
            result.setState(0);
            result.setMessage("");
            result.setData(data);
            return result ;
        }

    }

    @CrossOrigin(origins = "*")
    @RequestMapping("/newqgs")
    @ResponseBody
    public RestResult newQgs(
            String uid,
            String pid,
            String sid,
            String datetime,
            String sdui,
            String roiid,
            String styleid
    )
    {
        RestResult rr = new RestResult() ;
        String capurl = "" ;
        String wmsLayer = "" ;

        String omcApi = WConfig.getSharedInstance().omc_localhost_api ;

        //新新建一个qgs项目
        String relQgsfile = "" ;
        {
            HttpTool http1 = new HttpTool() ;
            int ret1 = http1.omcRpc(omcApi , "project.new" , "{}" ) ;
            if( ret1!=0 ){
                rr.setState(11);
                rr.setMessage(http1.getError());
                return rr ;
            }
            relQgsfile = ((Map)http1.getResult().get("data")).get("file").toString() ;
        }

        int ipid = Integer.parseInt(pid) ;
        int isid = Integer.parseInt(sid) ;

        WConfig c = WConfig.getSharedInstance() ;
        int maxZoom = 0 ;

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice() ;
        if( ipid > 0 ){
            //http://192.168.56.103:15900/pe/product/1/wmts/WMTSCapabilities.xml
            //layer layer_{pid}
            capurl = "http://" + c.host + ":" + c.port + "/pe/product/"+pid+"/wmts/WMTSCapabilities.xml" ;
            wmsLayer = "layer_"+pid ;

            JProduct pdt1 = rdb.rdbGetProductForAPI(ipid) ;
            if( pdt1==null ){
                rr.setState(2); rr.setMessage("can not find product info by "+pid);
                return rr ;
            }
            maxZoom = pdt1.maxZoom ;

        }else if( isid > 0 ){
            //http://192.168.56.103:15900/pe/scripts/1/wmts/WMTSCapabilities.xml
            //layer scirpt_{sid}
            capurl = "http://" + c.host + ":" + c.port + "/pe/scripts/"+sid+"/wmts/WMTSCapabilities.xml" ;
            wmsLayer = "scirpt_"+sid ;

            JScript script1 = rdb.rdbGetScript(isid) ;
            if(script1==null) {
                rr.setState(2); rr.setMessage("no script in db for sid "+sid);
                return rr ;
            }
            String absJsFile = c.pedir + script1.jsfile ;
            try{
                String scriptText = FileDirTool.readFileAsString(absJsFile) ;
                HBasePeHelperCppConnector cc = new HBasePeHelperCppConnector() ;
                String jsontext = cc.GetDatasetNameArray("com/pixelengine/HBasePixelEngineHelper" ,scriptText ) ;
                Gson gson = new Gson() ;
                JDsNameArrayResult dsArrResult = gson.fromJson( jsontext , JDsNameArrayResult.class ) ;
                if( dsArrResult.data.length  == 0 ){
                    throw new Exception("can not parse dataset name from script text("+sid+").") ;
                }else{
                    JProduct pdt2 = rdb.rdbGetProductInfoByName(dsArrResult.data[0]) ;
                    if( pdt2==null ) {
                        rr.setState(2); rr.setMessage("can not find product info by dsname:'"+dsArrResult.data[0]+"'.");
                        return rr ;
                    }
                    maxZoom = pdt2.maxZoom ;
                }
            }catch (Exception ex){
                rr.setState(2); rr.setMessage(ex.getMessage());
                return rr ;
            }
        }else{
            rr.setState(1);
            rr.setMessage("pid and sid both invalid.");
            return rr ;
        }

        String tms = "ms_" + String.valueOf(maxZoom) ;
        Map<String,String> map2 = new HashMap<>() ;
        map2.put("file" , relQgsfile) ;
        map2.put("capurl" , capurl) ;
        map2.put("tms" , tms) ;
        map2.put("layers", wmsLayer) ;
        map2.put("styleid" , styleid) ;
        map2.put("datetime" , datetime ) ;
        map2.put("sdui" , sdui) ;
        map2.put("roiid" , roiid) ;

        {
            Gson gson2 = new Gson () ;
            String jsondata2 = gson2.toJson(map2,Map.class) ;
            HttpTool http2 = new HttpTool() ;
            int ret2 = http2.omcRpc(omcApi , "project.addwms" , jsondata2) ;
            if( ret2==0 ){
                //写入数据库
                String currdt = rdb.getCurrentDatetimeStr() ;
                int ret3 = rdb.insertNewOmcFile(1,0,relQgsfile, Integer.valueOf(uid) , currdt ) ;
                if( ret3>=0 ){
                    rr.setState(0);
                    rr.setData( relQgsfile );
                    return rr ;
                }else{
                    rr.setState(30);
                    rr.setMessage("qgsfile failed to insert into db.");
                    return rr ;
                }

            }else{
                rr.setState(ret2);
                rr.setMessage( http2.getError());
                return rr ;
            }
        }
//        {
//            "file":"omc_out/20220417/122550-4499.qgs",
//                "capurl":"http://192.168.56.103:15900/pe/product/1/wmts/WMTSCapabilities.xml",
//                "tms":"ms_5",
//                "layers":"layer_1",
//                "styleid":2,
//                "datetime":20200600000000,
//                "sdui":"null",
//                "roiid":"sys:1"
//        }
    }


}
