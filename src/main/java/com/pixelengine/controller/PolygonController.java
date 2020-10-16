package com.pixelengine.controller;

import com.pixelengine.JRDBHelperForWebservice;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PolygonController {
    @ResponseBody
    @RequestMapping(value="/poly/list",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public ResponseEntity<byte[]> polylist( String uid)
    {
        System.out.println("/poly/list");
        int uid2 = Integer.parseInt(uid);

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        String outjson = rdb.rdbGetUserPolyList(uid2) ;

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<byte[]>( outjson.getBytes(), headers, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value="/poly/detail",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public ResponseEntity<byte[]> polydetail(String polyid)
    {
        System.out.println("/poly/detail");
        int polyid2 = Integer.parseInt(polyid);

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        String outjson = rdb.rdbGetPolyDetail(polyid2) ;

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<byte[]>( outjson.getBytes(), headers, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value="/poly/new",method= RequestMethod.POST)
    @CrossOrigin(origins = "*")
    public ResponseEntity<byte[]> polydetail(String uid,String geojson)
    {
        System.out.println("/poly/new");
        int uid2 = Integer.parseInt(uid);

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        int polyid = rdb.rdbSavePoly(uid2 , geojson) ;
        String outjson = "{\"result\":"+polyid + "}" ;
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<byte[]>( outjson.getBytes(), headers, HttpStatus.OK);
    }
}
