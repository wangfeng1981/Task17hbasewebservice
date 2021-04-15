package com.pixelengine.controller;

import com.google.gson.Gson;
import com.pixelengine.DataModel.MD5;
import com.pixelengine.DataModel.RestResult;
import com.pixelengine.JRDBHelperForWebservice;
import com.pixelengine.JUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Optional;

@RestController
public class UserController {
    @ResponseBody
    @RequestMapping(value="/user/login",method= RequestMethod.POST)
    @CrossOrigin(origins = "*")
    public RestResult userlogin(
            String uname, String password)
    {
        RestResult result = new RestResult() ;
        System.out.println("/user/login");
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        JUser user = rdb.rdbGetUserByUname(uname) ;
        if( user==null )
        {
            result.setState(1);
            result.setMessage("没有找到用户名:"+uname);
            return result ;
        }else
        {
            if( user.password.equals(password) )
            {
                String timestampStr = String.valueOf( (new Date()).getTime() ) ;
                String newtoken = MD5.getMd5(user.uname+timestampStr) ;
                user.token = newtoken ;
                JUser.addToSharedList(user);
                user.password = "" ;
                result.setState(0);
                result.setData(user);
                return result ;
            }else{
                result.setState(1);
                result.setMessage("密码错误.");
                return result ;
            }
        }
    }



    @ResponseBody
    @RequestMapping(value="/user/logout",method= RequestMethod.POST)
    @CrossOrigin(origins = "*")
    public RestResult userlogout(
            String token )
    {
        RestResult result = new RestResult() ;
        System.out.println("/user/logout");

        JUser tempUser = JUser.getUserByToken(token) ;
        if( tempUser==null ){
            result.setState(0) ;
            result.setMessage("用户没有登录或者登录信息已失效");
        }else{
            JUser.removeUserByToken( token );
            result.setState(0) ;
            result.setMessage("用户已登出");
        }
        return result ;
    }
}
