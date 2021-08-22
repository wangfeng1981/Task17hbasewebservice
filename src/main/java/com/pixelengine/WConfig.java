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
    public String hbaseuserfiletable;
    public String productwmts , host , port ;
    public String pedir ;
    public String userhtable, userhfami ;
    public int userhpidblen , useryxblen ;

    //2021-8-2 为了支持测试环境，支持本地瓦片local和大数据hbase两种
    public String tiletype ;//local or hbase
    public String tilelocalrootdir ;


}
