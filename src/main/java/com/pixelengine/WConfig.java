package com.pixelengine;

import com.google.gson.Gson;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class WConfig {
    public static WConfig sharedConfig = null ;
    public static void init(String jsonfile)
    {
        try
        {
            FileInputStream ins = new FileInputStream(jsonfile) ;
            Reader reader0 = new InputStreamReader(ins, "UTF-8");
            Gson gson0 = new Gson() ;
            sharedConfig = gson0.fromJson(reader0 , WConfig.class) ;
        }catch(Exception ex )
        {
            System.out.println("Error : failed to init wconfig.");
        }
    }
    public String zookeeper,sparkmaster,connstr,user,pwd;
    public String wmtsxml, wmtsxml2 ,tempdir , hbaseuserfiletable;
}
