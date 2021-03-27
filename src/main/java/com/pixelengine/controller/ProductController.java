package com.pixelengine.controller;
import com.pixelengine.*;
import com.pixelengine.DataModel.JProduct;
import com.pixelengine.DataModel.JProductDataItem;
import com.pixelengine.DataModel.RestResult;
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
public class ProductController {

    @ResponseBody
    @RequestMapping(value="/product/all",method=RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public RestResult productAll() throws IOException {
        RestResult rr = new RestResult() ;
        rr.setState(0);
        rr.setMessage("");

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();

        try{
            ArrayList<JProduct> productlist = rdb.rdbGetProducts() ;
            rr.setData(productlist);
        }catch (Exception ex){
            rr.setState(1);
            rr.setMessage("exception");
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

}
