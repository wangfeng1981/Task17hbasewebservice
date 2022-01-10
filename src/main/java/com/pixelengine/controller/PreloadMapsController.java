package com.pixelengine.controller;

import com.google.gson.Gson;
import com.pixelengine.DataModel.JPreloadMapsData;
import com.pixelengine.DataModel.JProduct;
import com.pixelengine.DataModel.JProductDisplay;
import com.pixelengine.DataModel.RestResult;
import com.pixelengine.JRDBHelperForWebservice;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;

@RestController
public class PreloadMapsController {

    @ResponseBody
    @RequestMapping(value="/preloadmaps/get",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult preloadmapsGet(String uid) throws IOException {
        RestResult rr = new RestResult() ;
        rr.setState(0);
        rr.setMessage("");

        try{
            JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
            JPreloadMapsData predata = rdb.rdbGetPreloadMapsDisplayId(Integer.valueOf(uid));
            String arrtext = predata.preloadlist ;
            Gson gson = new Gson() ;
            int[] dpidArray = gson.fromJson(arrtext , int[].class) ;
            ArrayList<JProduct> productList = new ArrayList<>();
            for(int idis = 0 ; idis < dpidArray.length; ++ idis )
            {
                JProductDisplay pd = rdb.rdbGetProductDisplayInfoByDisplayId(dpidArray[idis]) ;
                if(pd.pid > 0 && pd.type.compareTo("pe") == 0 ){
                    JProduct pinfo = rdb.rdbGetOneProductLayerInfoById(pd.pid  ) ;
                    productList.add(pinfo) ;
                }else{
                    JProduct emptyProduct = new JProduct() ;
                    emptyProduct.productDisplay = pd ;
                    productList.add(emptyProduct) ;
                }
            }
            rr.setState(0);
            rr.setData(productList);
        }catch (Exception ex){
            rr.setState(1);
            rr.setMessage("exception:" + ex.getMessage() );
        }
        return rr;
    }

    @ResponseBody
    @RequestMapping(value="/preloadmaps/set",method= RequestMethod.POST)
    @CrossOrigin(origins = "*")
    public RestResult preloadmapsSet(String uid,String preloadlist) throws IOException {
        RestResult rr = new RestResult() ;
        rr.setState(0);
        rr.setMessage("");
        try{
            int uidi = Integer.parseInt(uid) ;
            JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
            JPreloadMapsData predata = rdb.rdbGetPreloadMapsDisplayId(uidi);
            if( predata!=null && predata.uid == uidi )
            {//update
                boolean isok = rdb.rdbUpdatePreloadlist( predata.premapid , preloadlist) ;
                if( isok==true ){
                    rr.setState(0);
                    rr.setData(preloadlist);
                }else{
                    rr.setState(1);
                    rr.setMessage("preloadmapsSet update failed.");
                }
            }else{
                //insert
                boolean isok = rdb.rdbInsertPreloadlist(uidi , preloadlist);
                if( isok==true ){
                    rr.setState(0);
                    rr.setData(preloadlist);
                }else{
                    rr.setState(1);
                    rr.setMessage("preloadmapsSet insert failed.");
                }
            }
        }catch (Exception ex){
            rr.setState(1);
            rr.setMessage("exception:" + ex.getMessage() );
        }
        return rr ;
    }

    @ResponseBody
    @RequestMapping(value="/preloadmaps/remove",method= RequestMethod.POST)
    @CrossOrigin(origins = "*")
    public RestResult preloadmapsDelete(String uid) throws IOException {
        RestResult rr = new RestResult() ;
        rr.setState(0);
        rr.setMessage("");
        try{
            int uidi = Integer.parseInt(uid) ;
            JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
            boolean isok = rdb.rdbDeletePreloadlist(Integer.valueOf(uid)) ;
            if( isok==true ){
                rr.setState(0);
            }else{
                rr.setData(1);
                rr.setMessage("preloadmapsDelete failed to delete");
            }
        }catch (Exception ex){
            rr.setState(1);
            rr.setMessage("exception:" + ex.getMessage() );
        }
        return rr ;
    }




}
