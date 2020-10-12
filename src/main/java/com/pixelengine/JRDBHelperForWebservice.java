package com.pixelengine;

import com.google.gson.Gson;


import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;


/// 关系数据库交互
public class JRDBHelperForWebservice {
    public static WConfig wconfig =null ;
    public static Connection connection = null ;
    private static Hashtable<String,JProductInfo> productInfoPool =new Hashtable<String,JProductInfo>() ;
    private static Hashtable<Integer,String> productPidNamePool =new Hashtable<Integer,String>() ;

    private long getCurrentDatetime(){
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String formattedDate = myDateObj.format(myFormatObj);
        return Long.parseLong(formattedDate);
    }

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


    public int rdbNewRenderTask( String script, String styleJsonStr, int uid) {
        try
        {
            String query = " insert into tbRenderTask (scriptContent, renderStyle, uid)"
                    + " values (?, ?, ?)";
            // create the mysql insert preparedstatement
            PreparedStatement preparedStmt = JRDBHelperForWebservice.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            preparedStmt.setString (1, script);
            preparedStmt.setString (2, styleJsonStr);
            preparedStmt.setInt    (3, uid);
            // execute the preparedstatement
            preparedStmt.executeUpdate();
            ResultSet rs = preparedStmt.getGeneratedKeys();
            int last_inserted_id = -1 ;
            if(rs.next())
            {
                last_inserted_id = rs.getInt(1);
            }
            return last_inserted_id;
        }catch (Exception ex )
        {
            System.out.println("Error : rdbNewRenderTask exception , " + ex.getMessage() ) ;
            return -1 ;
        }

    }

    public int rdbNewUserScript( int uid, String script0){
        try
        {
            long dt0 = this.getCurrentDatetime();
            String query = " insert into tbScript (title, scriptContent, updateTime, uid)"
                    + " values (?, ?, ?, ?)";
            // create the mysql insert preparedstatement
            PreparedStatement preparedStmt = JRDBHelperForWebservice.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            preparedStmt.setString (1, "no-title");
            preparedStmt.setString (2, script0);
            preparedStmt.setLong   (3, dt0);
            preparedStmt.setInt    (4, uid);
            // execute the preparedstatement
            preparedStmt.executeUpdate();
            ResultSet rs = preparedStmt.getGeneratedKeys();
            int last_inserted_id = -1 ;
            if(rs.next())
            {
                last_inserted_id = rs.getInt(1);
                //update title
                String newtitle = "script-" + last_inserted_id;
                String query2 = "update tbScript set title = ? where sid = ?";
                PreparedStatement preparedStmt2 = JRDBHelperForWebservice.getConnection().prepareStatement(query2);
                preparedStmt2.setString   (1, newtitle);
                preparedStmt2.setInt      (2, last_inserted_id);
                preparedStmt2.executeUpdate();
            }
            return last_inserted_id;
        }catch (Exception ex )
        {
            System.out.println("Error : rdbNewRenderTask exception , " + ex.getMessage() ) ;
            return -1 ;
        }
    }

    public String rdbGetUserScriptListJson(int uid)
    {
        try {
            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery("SELECT sid,title,updateTime,uid "
                    +" FROM tbScript WHERE uid="+uid+" LIMIT 200") ;
            Gson gson = new Gson();
            String outjson = "{\"scripts\":[" ;
            int nrec = 0 ;
            while (rs.next()) {
                JScript jscript = new JScript();
                jscript.sid = rs.getInt("sid");
                jscript.uid = rs.getInt("uid");
                jscript.title = rs.getString("title");
                jscript.scriptContent = "...";
                jscript.updateTime = rs.getLong("updateTime");

                String onejson = gson.toJson(jscript, JScript.class) ;
                if( nrec>0 ){
                    outjson+=",";
                }
                outjson+=onejson;
                ++nrec;
            }
            outjson +="]}" ;
            return outjson;
        } catch (SQLException e) {
            System.out.println(e.getMessage()) ;
            String outjson = "{\"scripts\":[]}" ;
            return outjson ;
        }
    }

    public JScript rdbGetUserScript(int sid)
    {
        try {
            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery("SELECT sid,title,scriptContent,updateTime,uid "
                    +" FROM tbScript WHERE sid="+sid+" LIMIT 1") ;
            if (rs.next()) {
                JScript jscript = new JScript();
                jscript.sid = rs.getInt("sid");
                jscript.uid = rs.getInt("uid");
                jscript.title = rs.getString("title");
                jscript.scriptContent = rs.getString("scriptContent");
                jscript.updateTime = rs.getLong("updateTime");
                return jscript;
            }
            return null;
        } catch (SQLException e) {
            System.out.println(e.getMessage()) ;
            return null ;
        }
    }

    public void rdbUpdateUserScript( int sid, String script, String title ){
        if( script != null || title != null )
        {
            try
            {
                long dt0 = this.getCurrentDatetime();

                if( script!=null && title != null )
                {
                    String query2 = "update tbScript set title = ?, scriptContent = ? , updateTime = ?  where sid = ?";
                    PreparedStatement preparedStmt2 = JRDBHelperForWebservice.getConnection().prepareStatement(query2);
                    preparedStmt2.setString   (1, title);
                    preparedStmt2.setString   (2, script);
                    preparedStmt2.setLong     (3, dt0);
                    preparedStmt2.setInt      (4, sid);
                    preparedStmt2.executeUpdate();
                }
                else if( script!=null )
                {
                    String query2 = "update tbScript set scriptContent = ? , updateTime = ?  where sid = ?";
                    PreparedStatement preparedStmt2 = JRDBHelperForWebservice.getConnection().prepareStatement(query2);
                    preparedStmt2.setString   (1, script);
                    preparedStmt2.setLong     (2, dt0);
                    preparedStmt2.setInt      (3, sid);
                    preparedStmt2.executeUpdate();
                }else
                {
                    String query2 = "update tbScript set title = ? , updateTime = ?  where sid = ?";
                    PreparedStatement preparedStmt2 = JRDBHelperForWebservice.getConnection().prepareStatement(query2);
                    preparedStmt2.setString   (1, title);
                    preparedStmt2.setLong     (2, dt0);
                    preparedStmt2.setInt      (3, sid);
                    preparedStmt2.executeUpdate();
                }
            }catch (Exception ex )
            {
                System.out.println("Error : rdbNewRenderTask exception , " + ex.getMessage() ) ;
            }
        }
    }

}
