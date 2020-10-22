package com.pixelengine.controller;

import com.google.gson.Gson;
import com.pixelengine.JRDBHelperForWebservice;
import com.pixelengine.JScript;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
public class ScriptsController {

    @ResponseBody
    @RequestMapping(value="/scripts/user/new/{uid}",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public ResponseEntity<byte[]> scriptnew(@PathVariable String uid , String type)
    {
        System.out.println("/scripts/user/new/{uid}");
        int uid2 = Integer.parseInt(uid) ;
        String scriptZero="";
        if( type.compareTo("1")==0 )
        {
            scriptZero = "function main(){\n  return null;\n}" ;
        }else
        {
            scriptZero = "function zlevelFunc()\n{\n  return 1;\n}\nfunction extentFunc()\n{\n  return [110.0,120.0,35.0,32.0];//left,right,top,bottom\n}\nfunction sharedobjectFunc()\n{\n  return {};\n}\nfunction mapFunc( sharedobj )\n{\n  return {key:\"somekey\", val:{data:1}} ;\n}\nfunction reduceFunc( sharedObj, key, obj1, obj2 )\n{\n  var sum=obj1.data+obj2.data;\n  return {data:sum};\n}\nfunction main( objCollection )\n{\n  var key0sum= objCollection[0].data;\n  return {data:key0sum};\n}";
        }
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        int newsid = rdb.rdbNewUserScript(uid2,scriptZero, Integer.parseInt(type)) ;
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String outjson = "{\"sid\":" + newsid + ", \"type\":" +type+ "}" ;
        return new ResponseEntity<byte[]>( outjson.getBytes(), headers, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value="/scripts/user/{uid}",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public ResponseEntity<byte[]> scriptlist(@PathVariable String uid)
    {
        System.out.println("/scripts/user/{uid}");
        int uid2 = Integer.parseInt(uid);

        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        String outjson = rdb.rdbGetUserScriptListJson(uid2) ;

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<byte[]>( outjson.getBytes(), headers, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value="/scripts/{sid}",method= RequestMethod.GET)
    @CrossOrigin(origins = "*")
    public ResponseEntity<byte[]> scriptdetail(@PathVariable String sid)
    {
        System.out.println("/scripts/{sid}");
        JRDBHelperForWebservice rdb =new JRDBHelperForWebservice();
        JScript sc = rdb.rdbGetUserScript(Integer.parseInt(sid)) ;
        if( sc==null ){
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity<byte[]>( "{}".getBytes(), headers, HttpStatus.OK);
        }else
        {
            Gson gson = new Gson();
            String outjson = gson.toJson(sc,JScript.class);
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity<byte[]>( outjson.getBytes(), headers, HttpStatus.OK);
        }
    }

    @ResponseBody
    @RequestMapping(value="/scripts/update/{sid}",method= RequestMethod.POST)
    @CrossOrigin(origins = "*")
    public ResponseEntity<byte[]> scriptupdate(
            @PathVariable String sid,
            @RequestParam("script") Optional<String> script,
            @RequestParam("title") Optional<String> title)
    {
        System.out.println("/scripts/update/{sid}");
        int sid2 = Integer.parseInt(sid);
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        if( script.isPresent() && title.isPresent() )
        {
            rdb.rdbUpdateUserScript(sid2,script.get(),title.get());
        }else if( title.isPresent() )
        {
            rdb.rdbUpdateUserScript(sid2,null,title.get());
        }else if( script.isPresent() )
        {
            rdb.rdbUpdateUserScript(sid2,script.get(),null);
        }
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<byte[]>( "{\"status\":0}".getBytes(), headers, HttpStatus.OK);
    }
}
