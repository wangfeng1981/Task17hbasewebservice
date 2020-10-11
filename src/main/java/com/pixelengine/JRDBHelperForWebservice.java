package com.pixelengine;

import com.google.gson.Gson;


import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;


/// 关系数据库交互
public class JRDBHelperForWebservice {
    public static WConfig wconfig =null ;
    public static Connection connection = null ;
    private static Hashtable<String,JProductInfo> productInfoPool =new Hashtable<String,JProductInfo>() ;
    private static Hashtable<Integer,String> productPidNamePool =new Hashtable<Integer,String>() ;

    public static void init(WConfig twconfig)
    {
        wconfig = twconfig;

    }

    private static WConfig getWConfig() {
        return wconfig;
    }
    private static Connection getConnection() {
        try{
            if( connection==null || connection.isClosed() ){
                System.out.println("create connection ..." );
                connection = DriverManager
                        .getConnection( getWConfig().connstr, getWConfig().user, getWConfig().pwd);
                System.out.println("create connection ok." );
            }
            return connection;
        }catch (Exception e){
            System.out.println("Error : create connection failed, " + e.getMessage() );
            return null ;
        }
    }

    public JProductInfo rdbGetProductInfoByName(String dsname)   {
        JProductInfo pinfo1 = productInfoPool.get(dsname) ;
        if( pinfo1==null  )
        {
            try {
                Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM tbProduct WHERE productName='"+dsname+"' LIMIT 1") ;
                if (rs.next()) {
                    int pid = rs.getInt("pid");
                    int uid = rs.getInt("uid");
                    String info = rs.getString("productInfo");
                    System.out.println("=== find dsname,pid : "+dsname+","+pid);
                    JProductInfo pinfo = new Gson().fromJson(info, JProductInfo.class) ;
                    synchronized(this){
                        if( productInfoPool.size() > 1000 ){
                            Integer somepid = productPidNamePool.keys().nextElement();
                            String somePname = productPidNamePool.get(somepid) ;
                            productPidNamePool.remove(somepid) ;
                            productInfoPool.remove(somePname) ;//remove a random one.
                        }
                        productInfoPool.put(dsname,pinfo) ;
                        productPidNamePool.put( pinfo.pid , dsname) ;
                    }
                    return pinfo;
                }else{
                    System.out.println("Error : not find productInfo of "+ dsname);
                    return null ;
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage()) ;
                return null ;
            }
        }else{
            return pinfo1 ;
        }
    }

    public JProductInfo rdbGetProductInfoByMysqlPid(int mysqlPid)   {
        String pname = productPidNamePool.get(mysqlPid) ;
        JProductInfo pinfo1 = null;
        if( pname !=null )
        {
            pinfo1 = productInfoPool.get(pname) ;
        }

        if( pinfo1 == null )
        {
            try {
                Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM tbProduct WHERE pid="+mysqlPid+" LIMIT 1") ;
                if (rs.next()) {
                    int pid = rs.getInt("pid");
                    int uid = rs.getInt("uid");
                    String info = rs.getString("productInfo");
                    System.out.println("=== find product by mysql-pid : "+pid);
                    JProductInfo pinfo = new Gson().fromJson(info, JProductInfo.class) ;
                    synchronized(this){
                        if( productInfoPool.size() > 1000 ){
                            Integer somepid = productPidNamePool.keys().nextElement();
                            String somePname = productPidNamePool.get(somepid) ;
                            productPidNamePool.remove(somepid) ;
                            productInfoPool.remove(somePname) ;//remove a random one.
                        }
                        productInfoPool.put(pinfo.productName,pinfo) ;
                        productPidNamePool.put( pinfo.pid , pinfo.productName) ;
                    }
                    return pinfo;
                }else{
                    System.out.println("Error : not find productInfo by mysql-pid of "+ mysqlPid);
                    return null ;
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage()) ;
                return null ;
            }
        }else{
            return pinfo1;
        }
    }


    public JRenderTask rdbGetRenderTask( int rid)
    {
        JRenderTask task = null;
        try {
            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM tbRenderTask WHERE rid="+rid+" LIMIT 1") ;
            if (rs.next()) {
                task = new JRenderTask() ;
                task.rid = rs.getInt(1) ;
                task.scriptContent = rs.getString(2) ;
                task.renderStyle = rs.getString(3) ;
                task.uid = rs.getInt(4);
                task.desc = rs.getString(5) ;
                return task;
            }else{
                System.out.println("Error : not find JRenderTask by "+ rid);
                return null ;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage()) ;
            return null ;
        }
    }
}
