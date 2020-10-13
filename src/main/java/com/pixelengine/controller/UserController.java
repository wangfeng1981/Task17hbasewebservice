package com.pixelengine.controller;

import com.google.gson.Gson;
import com.pixelengine.JRDBHelperForWebservice;
import com.pixelengine.JUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
public class UserController {
    @ResponseBody
    @RequestMapping(value="/user/login",method= RequestMethod.POST)
    @CrossOrigin(origins = "*")
    public ResponseEntity<byte[]> scriptupdate(
            String uname)
    {
        System.out.println("/user/login");

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        JUser user = rdb.rdbGetUserByUname(uname) ;
        if( user==null )
        {
            user = new JUser();
            user.uid = 0 ;
            user.uname = "failed" ;
            Gson gson = new Gson() ;
            String outjson = gson.toJson(user , JUser.class );
            return new ResponseEntity<byte[]>( outjson.getBytes(), headers, HttpStatus.OK);
        }else
        {
            Gson gson = new Gson() ;
            String outjson = gson.toJson(user , JUser.class );
            return new ResponseEntity<byte[]>( outjson.getBytes(), headers, HttpStatus.OK);
        }
    }
}
