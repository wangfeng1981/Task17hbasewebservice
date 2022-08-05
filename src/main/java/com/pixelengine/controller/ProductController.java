package com.pixelengine.controller;
import com.pixelengine.*;
import com.pixelengine.DataModel.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
//update 2022-4-9
//2022-7-31 categories2
//2022-8-3 displayid
//2022-8-5 refreshcache

//获取全部产品信息
@RestController
public class ProductController {
    final public static String MyProductCatDisplayId  = "myproduct";
    final public static String MyScriptCatDisplayId  = "myscript";
    final public static String SearchCatDisplayId = "search";

    private int getIndexOfLvl1(JProductCategory2 cat2,int lvl1pk ){
        for(int i = 0 ; i<cat2.level1Array.size();++i ){
            if( cat2.level1Array.get(i).meta.pk==lvl1pk){
                return i ;
            }
        }
        return -1 ;
    }

    //按分类获取全部产品 2022-7-31
    @ResponseBody
    @RequestMapping(value="/product/categories2",method=RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult allCategoryProducts2(String uid) {
        RestResult rr = new RestResult() ;
        rr.setState(0);
        rr.setMessage("");
        try {
            JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
            JProductCategory2 cate2Object = new JProductCategory2() ;

            ArrayList<JMeta> level1array = rdb.getMetaByKey("CATLVL1");
            ArrayList<JMeta> level2array = rdb.getMetaByKey("CATLVL2");
            for(int i1 = 0 ; i1 < level1array.size();++i1 ){
                JProductCategory2.CateLevel1 lvl1 = new JProductCategory2.CateLevel1();
                lvl1.meta = level1array.get(i1) ;
                lvl1.displayid = String.valueOf(lvl1.meta.pk) ;
                cate2Object.level1Array.add(lvl1) ;
            }
            for(int i2 = 0;i2<level2array.size();++i2){
                int ilvl1  = getIndexOfLvl1(cate2Object , level2array.get(i2).theid ) ;
                if( ilvl1>=0 ){
                    JProductCategory2.CateLevel2 lvl2= new JProductCategory2.CateLevel2();
                    lvl2.meta = level2array.get(i2) ;
                    lvl2.displayid = String.valueOf(lvl2.meta.pk) ;
                    String key2 = "CATLVL3_" + String.valueOf(lvl2.meta.pk) ;
                    ArrayList<JMeta> level3pdtarray  =  rdb.getMetaByKey(key2) ;
                    for(int i3=0;i3<level3pdtarray.size();++i3){
                        JProductDisplay display1 =
                                rdb.rdbGetProductDisplayInfoByDisplayId( level3pdtarray.get(i3).theid );
                        if (display1.pid > 0 && display1.type.equals("pe") ) {
                            JProduct pinfo = rdb.rdbGetOneProductLayerInfoById(display1.pid);
                            pinfo.displayid = display1.type + String.valueOf(display1.dpid);
                            lvl2.productArray.add(pinfo) ;
                        } else {
                            JProduct emptyProduct = new JProduct();
                            emptyProduct.productDisplay = display1;
                            emptyProduct.displayid = display1.type + String.valueOf(display1.dpid);
                            lvl2.productArray.add(emptyProduct);
                        }
                    }
                    cate2Object.level1Array.get(ilvl1).level2Array.add(lvl2) ;
                }
            }

            //my products
            {
                JProductCategory2.CateLevel1 l1 = new JProductCategory2.CateLevel1();
                l1.meta = new JMeta() ;
                l1.displayid = MyProductCatDisplayId;
                l1.meta.pk = 0 ;
                l1.meta.theid = 0 ;
                l1.meta.metakey = "CATLVL1" ;
                l1.meta.metavali = 0 ;
                l1.meta.metavalstr = "我的" ;

                JProductCategory2.CateLevel2 lvl2 = new JProductCategory2.CateLevel2();
                lvl2.meta = new JMeta();
                lvl2.displayid = MyScriptCatDisplayId ;
                lvl2.meta.pk=0;
                lvl2.meta.metavalstr = "我的脚本" ;
                lvl2.meta.metavali = 0 ;
                lvl2.meta.theid = 0 ;
                lvl2.meta.metakey = "CATLVL2" ;

                //MyScript products
                if( Integer.valueOf(uid)>0 ){
                    ArrayList<JScript> list = rdb.rdbGetUserScriptList(Integer.valueOf(uid)) ;
                    if( list!=null ){
                        for(int iscript = 0 ; iscript < list.size(); ++ iscript){
                            JProduct pdt1 = list.get(iscript).convert2UsJProductWithDisplay();
                            pdt1.displayid = "us" + String.valueOf(list.get(iscript).sid) ;
                            lvl2.productArray.add(pdt1) ;
                        }
                    }
                }

                l1.level2Array.add(lvl2) ;
                cate2Object.level1Array.add(l1) ;
            }

            //search , yes is empty now
            {
                JProductCategory2.CateLevel1 l1 = new JProductCategory2.CateLevel1();
                l1.meta = new JMeta() ;
                l1.displayid = SearchCatDisplayId ;
                l1.meta.pk = 0 ;
                l1.meta.theid = 0 ;
                l1.meta.metakey = "CATLVL1" ;
                l1.meta.metavali = 0 ;
                l1.meta.metavalstr = "搜索" ;

                JProductCategory2.CateLevel2 lvl2 = new JProductCategory2.CateLevel2();
                lvl2.meta = new JMeta();
                lvl2.displayid = SearchCatDisplayId ;
                lvl2.meta.pk=0;
                lvl2.meta.metavalstr = "搜索" ;
                lvl2.meta.metavali = 0 ;
                lvl2.meta.theid = 0 ;
                lvl2.meta.metakey = "CATLVL2" ;

                l1.level2Array.add(lvl2) ;
                cate2Object.level1Array.add(l1) ;
            }


            rr.setData(cate2Object);
            return rr ;
        }catch (Exception ex){
            rr.setState(1);
            rr.setMessage("Exception:"+ex.getMessage());
            return rr ;
        }
    }



    //search product and myscript by key, the key str is as whole one key.
    @ResponseBody
    @RequestMapping(value="/product/search",method=RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult search( String key, String uid) {
        RestResult rr = new RestResult();
        rr.setState(0);
        rr.setMessage("");
        try {
            JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
            ArrayList<Integer> searchDpidArr = rdb.searchProductDisplay(key) ;
            ArrayList<JProduct> pdtArr = new ArrayList<>();
            for(int i = 0 ; i<searchDpidArr.size();++i ){
                JProductDisplay display1 =
                        rdb.rdbGetProductDisplayInfoByDisplayId( searchDpidArr.get(i) );
                if (display1.pid > 0 && display1.type.equals("pe") ) {
                    JProduct pinfo = rdb.rdbGetOneProductLayerInfoById(display1.pid);
                    pinfo.displayid = pinfo.productDisplay.type + String.valueOf(display1.dpid);
                    pdtArr.add(pinfo) ;
                } else {
                    JProduct emptyProduct = new JProduct();
                    emptyProduct.productDisplay = display1;
                    pdtArr.add(emptyProduct);
                }
            }
            rr.setData(pdtArr);
            return rr;
        }catch (Exception ex){
            rr.setState(1);
            rr.setMessage("failed:"+ex.getMessage());
            return rr;
        }
    }



    //按分类获取全部产品2021-11-28
    @ResponseBody
    @RequestMapping(value="/product/categories",method=RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult allCategoryProducts() {
        RestResult rr = new RestResult() ;
        rr.setState(0);
        rr.setMessage("");

        try {
            JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
            ArrayList<JCategory> categories = rdb.rdbGetCategories();
            if (categories == null) {
                rr.setState(1);
                rr.setMessage("failed, rdb.rdbGetCategories() return null.");
                return rr;
            } else {
                for (int ic = 0; ic < categories.size(); ++ic) {
                    ArrayList<Integer> dpidArr = rdb.rdbGetCategoryProductDisplayIdList(categories.get(ic).catid);
                    if (dpidArr != null) {
                        ArrayList<JProduct> productList = new ArrayList<>();
                        for (int idp = 0; idp < dpidArr.size(); ++idp) {
                            JProductDisplay pd = rdb.rdbGetProductDisplayInfoByDisplayId(dpidArr.get(idp));
                            if (pd.pid > 0 && pd.type.compareTo("pe") == 0) {
                                JProduct pinfo = rdb.rdbGetOneProductLayerInfoById(pd.pid);
                                productList.add(pinfo);
                            } else {
                                JProduct emptyProduct = new JProduct();
                                emptyProduct.productDisplay = pd;
                                productList.add(emptyProduct);
                            }
                        }
                        categories.get(ic).products = productList;
                    }
                }
                rr.setState(0);
                rr.setData(categories);
                return rr;
            }
        }catch (Exception ex){
            rr.setState(1);
            rr.setMessage("failed, ProductController.allCategoryProducts exception:"+ex.getMessage());
            return rr;
        }
    }

    //增加xyz的图层产品
    @ResponseBody
    @RequestMapping(value="/product/all",method=RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult productAll() throws IOException {
        RestResult rr = new RestResult() ;
        rr.setState(0);
        rr.setMessage("");

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        ArrayList<Integer> dpidArr = rdb.rdbGetAllDisplayProduct() ;
        if( dpidArr!=null )
        {
            ArrayList<JProduct> productList = new ArrayList<>();
            for(int idis = 0 ; idis < dpidArr.size(); ++ idis )
            {
                JProductDisplay pd = rdb.rdbGetProductDisplayInfoByDisplayId(dpidArr.get(idis)) ;
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

        }else{
            rr.setState(1);
            rr.setMessage("failed, rdb.rdbGetAllDisplayProduct is null.");
        }
        return rr;
    }

    @ResponseBody
    @RequestMapping(value="/product/dataitemlist",method=RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult productDataItemList(String pid,
                                          String page,
                                          String pagesize,
                                          String order     //ASC/DESC
    ) throws IOException {
        RestResult rr = new RestResult() ;
        rr.setState(0);
        rr.setMessage("");

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        int pidval = Integer.parseInt(pid) ;
        int ipage = Integer.parseInt(page) ;
        int ipagesize = Integer.parseInt(pagesize) ;

        try{
            ArrayList<JProductDataItem> datalist = rdb.rdbGetProductDataItemList(
                    pidval,ipage,ipagesize,order) ;
            rr.setData(datalist);
        }catch (Exception ex){
            rr.setState(1);
            rr.setMessage("exception");
        }
        return rr;
    }

    @ResponseBody
    @RequestMapping(value="/product/yearlist",method=RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult getYearList(String pid
    ) throws IOException {
        RestResult rr = new RestResult() ;
        rr.setState(0);
        rr.setMessage("");

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        int pidval = Integer.parseInt(pid) ;
        try{
            ArrayList<Integer> datalist = rdb.rdbGetProductYearList(
                    pidval) ;
            rr.setData(datalist);
        }catch (Exception ex){
            rr.setState(1);
            rr.setMessage("exception");
        }
        return rr;
    }

    @ResponseBody
    @RequestMapping(value="/product/monthlist",method=RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult getMonthList(String pid,
                                  String year
    ) throws IOException {
        RestResult rr = new RestResult() ;
        rr.setState(0);
        rr.setMessage("");

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        int pidval = Integer.parseInt(pid) ;
        int yearval = Integer.parseInt(year) ;
        try{
            ArrayList<Integer> datalist = rdb.rdbGetProductMonthList(
                    pidval,yearval) ;
            rr.setData(datalist);
        }catch (Exception ex){
            rr.setState(1);
            rr.setMessage("exception");
        }
        return rr;
    }

    @ResponseBody
    @RequestMapping(value="/product/monthdataitemlist",method=RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult getMonthDataItemList(
            String pid,
            String year,
            String month
    ) throws IOException {
        RestResult rr = new RestResult() ;
        rr.setState(0);
        rr.setMessage("");

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        int pidval = Integer.parseInt(pid) ;
        int yearval = Integer.parseInt(year) ;
        int monval = Integer.parseInt(month) ;
        try{
            ArrayList<JProductDataItem> datalist = rdb.rdbGetProductMonthDataItemList(
                    pidval,yearval,monval) ;
            rr.setData(datalist);
        }catch (Exception ex){
            rr.setState(1);
            rr.setMessage("exception");
        }
        return rr;
    }


    //获取一个产品信息 2022-4-9
    @ResponseBody
    @RequestMapping(value="/product/info",method=RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult oneProductInfo(String pid) {
        RestResult rr = new RestResult() ;
        rr.setState(0);
        rr.setMessage("");

        try {
            JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
            JProduct oneinfo = rdb.rdbGetOneProductLayerInfoById( Integer.valueOf(pid)) ;
            if( oneinfo==null ){
                rr.setState(2);
                rr.setMessage("failed, ProductController.oneProductInfo no product info exception.");
                return rr ;
            }else{
                rr.setState(0);
                rr.setData(oneinfo);
                return rr ;
            }
        }catch (Exception ex){
            rr.setState(1);
            rr.setMessage("failed, ProductController.oneProductInfo exception:"+ex.getMessage());
            return rr;
        }
    }


    //刷新产品信息 2022-8-5
    @ResponseBody
    @RequestMapping(value="/product/refreshcache",method=RequestMethod.POST)
    @CrossOrigin(origins = "*")
    public RestResult refreshcache(String uname,String pwd) {
        JRDBHelperForWebservice.clearProductPool();
        RestResult rr = new RestResult() ;
        rr.setState(0);
        rr.setMessage("");
        rr.setData("refresh ok.");
        return rr ;
    }
}
