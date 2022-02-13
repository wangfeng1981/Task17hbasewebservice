package com.pixelengine.controller;
//这个还没想好怎么改 2021-4-1 这个不再使用，请使用ZonalStatController，或者参考ZonalStatController修改。
import org.springframework.web.bind.annotation.*;

@RestController
public class OffTaskController {

    //提交一个离线计算任务，瓦片计算
    //params scriptcontent
    //params path
    //params uid
    //params task description
//    @ResponseBody
//    @RequestMapping(value="/offtask1/new",method= RequestMethod.POST)
//    @CrossOrigin(origins = "*")
//    public RestResult onlineTaskNew(String script, String uid, String path, String dt) {
//        int uid1 = Integer.parseInt(uid) ;
//        long dt1 = Long.parseLong(dt);
//        int hpid = uid1; //use userid as hpid, 4-byte len
//        long hcol = JRDBHelperForWebservice.sgetCurrentDatetime();
//        path = "/u" + uid + path;
//
//        //
//        HBasePeHelperCppConnector cv8 = new HBasePeHelperCppConnector();
//        String errorText = cv8.CheckScriptOk( "com/pixelengine/HBasePixelEngineHelper", script) ;
//        final HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        if( errorText.compareTo("")!=0 )
//        {
//            System.out.println("Error : CheckScriptOk bad , " + errorText);
//            String outjson = "{\"oftid\":-1,\"message\":\"" + errorText+"\"}" ;
//            return new ResponseEntity<byte[]>( outjson.getBytes(), headers, HttpStatus.OK);
//        }
//        //入库 tbOfflineTask
//        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
//        if( rdb.rdbGetOffTask1DetailByPath(path) != null ){
//            String outjson = "{\"oftid\":-1,\"message\":\"has the same path\"}" ;
//            return new ResponseEntity<byte[]>( outjson.getBytes(), headers, HttpStatus.OK);
//        }
//
//
//        int the_oftid = rdb.rdbNewOffTask1(script,uid,path,dt);
//        if( the_oftid<0 ){
//            System.out.println("Error : failed insert offtask.");
//            String outjson = "{\"oftid\":-1,\"message\":\"failed to insert offtask.\"}" ;
//            return new ResponseEntity<byte[]>( outjson.getBytes(), headers, HttpStatus.OK);
//        }
//        //generate produce json
//        String jsontext = rdb.rdbGetOffTaskJson(the_oftid);
//        String tempjsonfile = WConfig.sharedConfig.tempdir + "offtask-"
//                + String.valueOf(JRDBHelperForWebservice.sgetCurrentDatetime())
//                + ".json" ;
//        if( jsontext==null ){
//            String outjson = "{\"oftid\":-1,\"message\":\"not found offtask record "+ the_oftid +"\"}" ;
//            return new ResponseEntity<byte[]>( outjson.getBytes(), headers, HttpStatus.OK);
//        }
//        try{
//            FileWriter filewriter = new FileWriter(tempjsonfile) ;
//            filewriter.write(jsontext);
//            filewriter.close();
//        }catch (Exception ex){
//            String outjson = "{\"oftid\":-1,\"message\":\"failed to write json\"}" ;
//            return new ResponseEntity<byte[]>( outjson.getBytes(), headers, HttpStatus.OK);
//        }
//        //call off task executor in other process without wait.

//        //done
//        String outjson = "{\"oltid\":"+the_oftid+",\"message\":\"\"}" ;
//        return new ResponseEntity<byte[]>( outjson.getBytes(), headers, HttpStatus.OK);
//        RestResult rr = new RestResult() ;
//        rr.setState(1);
//        rr.setMessage("not supported.");
//        return rr ;
//    }



    //submit a offlinetask of mapreduce style
    //params scriptcontent
    //params uid
    //params task description
    
}
