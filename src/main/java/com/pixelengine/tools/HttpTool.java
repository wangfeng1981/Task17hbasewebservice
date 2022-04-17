package com.pixelengine.tools;
//2022-4-17 created


import com.google.gson.Gson;
import com.jayway.restassured.response.Response;
import org.apache.http.NameValuePair;
import com.jayway.restassured.* ;


import java.io.*;

import java.util.*;

import static com.jayway.restassured.RestAssured.given;

public class HttpTool {
    public int omcRpc( String url1, String method, String jsondata ) {
        try{

            Response rep = given().log().everything(false)
                    .contentType("multipart/form-data")
                    .multiPart("method",method)
                    .multiPart("data",jsondata)
                    .post(url1) ;

            int code = rep.getStatusCode() ;
            if( code!= 200 ){
                error = "rpc return code:" + code ;
                return 8 ;
            }else{
                String jsonresult = rep.getBody().print() ;
                Gson gson = new Gson() ;
                result = new HashMap() ;
                result = gson.fromJson(jsonresult , Map.class) ;
                int code2 = (int)Double.parseDouble(result.get("state").toString() ) ;
                if( code2 == 0){
                    return 0 ;
                }else{
                    error = result.get("message").toString() ;
                    return code2 ;
                }
            }
        }catch(Exception ex){
            error = ex.getMessage() ;
            return 9 ;
        }
    }

    public String getError() {
        return error ;
    }

    public Map getResult() {
        return result ;
    }

    private String error ;
    private Map result ;


}
