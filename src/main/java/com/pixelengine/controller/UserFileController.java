package com.pixelengine.controller;

import com.pixelengine.JRDBHelperForWebservice;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserFileController {

    @ResponseBody
    @RequestMapping(value="/userfile/list",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public ResponseEntity<byte[]> userfilelist( String uid)
    {
        System.out.println("/userfile/list");
        int uid2 = Integer.parseInt(uid);

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        String outjson = rdb.rdbGetUserFileList(uid2) ;

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<byte[]>( outjson.getBytes(), headers, HttpStatus.OK);
    }



}
