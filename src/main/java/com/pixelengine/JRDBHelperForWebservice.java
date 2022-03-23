package com.pixelengine;
////////////////////////////////////////////////////////
//
//
/// 这个接口是Java与关系数据库MYSQL交互的
//update 2022-2-13 2310
//update 2022-3-24 0459
//
/////////////////////////////////////////////////////////


import com.google.gson.Gson;

import com.pixelengine.DataModel.*;


import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;



public class JRDBHelperForWebservice {
    private static String connstr,user,pwd ;
    public static Connection connection = null ;
    private static Hashtable<String,JProduct> productInfoPool =new Hashtable<String,JProduct>() ;
    private static Hashtable<Integer,String> productPidNamePool =new Hashtable<Integer,String>() ;

    private long getCurrentDatetime(){
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String formattedDate = myDateObj.format(myFormatObj);
        return Long.parseLong(formattedDate);
    }

    public static long sgetCurrentDatetime(){
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String formattedDate = myDateObj.format(myFormatObj);
        return Long.parseLong(formattedDate);
    }

    public static String getCurrentDatetimeStr(){
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        String formattedDate = myDateObj.format(myFormatObj);
        return formattedDate;
    }

    public static void init(String connstr1,String user1,String pwd1)
    {
        JRDBHelperForWebservice.connstr = connstr1 ;
        JRDBHelperForWebservice.user = user1 ;
        JRDBHelperForWebservice.pwd = pwd1 ;
    }

    private static Connection getConnection() {
        try{
            if( connection==null || connection.isClosed() ){
                System.out.println("create mysql connection ..." );
                connection = DriverManager
                        .getConnection( JRDBHelperForWebservice.connstr,
                                JRDBHelperForWebservice.user,
                                JRDBHelperForWebservice.pwd);
                System.out.println("create connection ok." );
            }
            return connection;
        }catch (Exception e){
            System.out.println("Error : create connection failed, " + e.getMessage() );
            return null ;
        }
    }

    //获取全部可见的分类，不包括产品,2021-11-28
    public ArrayList<JCategory> rdbGetCategories()
    {
        try {
            ArrayList<JCategory> result = new ArrayList<>() ;

            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            String sqlstr = "SELECT * FROM tbcategory WHERE visible=1 ORDER BY iorder ASC";
            ResultSet rs = stmt.executeQuery(sqlstr) ;
            while (rs.next()) {
                JCategory r1 = new JCategory();
                r1.catid = rs.getInt("catid");
                r1.catname = rs.getString("catname");
                r1.visible = rs.getInt("visible");
                r1.iorder = rs.getInt("iorder");
                result.add(r1) ;
            }
            return result ;
        } catch (SQLException e) {
            System.out.println(e.getMessage()) ;
            return null ;
        }
    }

    //获取当前分类的全部可见产品 2021-11-28
    public ArrayList<Integer> rdbGetCategoryProductDisplayIdList(int catid)
    {
        try {
            ArrayList<Integer> results = new ArrayList<>() ;
            String query2 = "SELECT dpid FROM tbproductdisplay WHERE cat="+String.valueOf(catid)+" AND visible=1 Order by iorder ASC";
            Statement stmt2 = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rs = stmt2.executeQuery(query2 );
            while (rs.next()) {
                results.add(rs.getInt(1));
            }
            return results ;
        } catch (SQLException e) {
            System.out.println(e.getMessage()) ;
            return null ;
        }
    }


    public JProduct rdbGetProductInfoByName(String dsname)   {
        JProduct pinfo1 = productInfoPool.get(dsname) ;
        if( pinfo1==null  )
        {
            try {
                String productTable = "tbproduct" ;

                Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
                ResultSet rs = stmt.executeQuery("SELECT pid FROM "+productTable+" WHERE name='"+dsname+"' LIMIT 1") ;
                if (rs.next()) {
                    int pid = rs.getInt("pid");
                    JProduct newpdt = this.rdbGetProductForAPI(pid) ;
                    System.out.println("=== find dsname,pid : "+dsname+","+newpdt.name);
                    synchronized(this){
                        if( productInfoPool.size() > 1000 ){
                            Integer somepid = productPidNamePool.keys().nextElement();
                            String somePname = productPidNamePool.get(somepid) ;
                            productPidNamePool.remove(somepid) ;
                            productInfoPool.remove(somePname) ;//remove a random one.
                        }
                        productInfoPool.put(dsname,newpdt) ;
                        productPidNamePool.put( newpdt.pid , dsname) ;
                    }
                    return newpdt;
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

    //如果name为空字符串返回全部静态图层，否则做关键字检索并返回匹配的图层
    public ArrayList<JStaticMapLayerProduct> rdbGetStaticMapLayer(String name)   {
        try {
            ArrayList<JStaticMapLayerProduct> result = new ArrayList<>() ;

            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            String sqlstr = "SELECT * FROM tbstaticmaplayer ORDER BY iorder";
            if( name.equals("")==false )
            {
                sqlstr = "SELECT * FROM tbstaticmaplayer WHERE productname like '%"+name+ "%' ORDER BY iorder" ;
            }
            ResultSet rs = stmt.executeQuery(sqlstr) ;
            while (rs.next()) {
                JStaticMapLayerProduct sml = new JStaticMapLayerProduct();
                sml.cat = rs.getInt("cat") ;
                sml.iorder = rs.getInt("iorder") ;
                sml.layertype = rs.getString("layertype") ;
                sml.params = rs.getString("params") ;
                sml.productdescription = rs.getString("productdescription") ;
                sml.productname = rs.getString("productname") ;
                sml.smid = rs.getInt("smid") ;
                sml.thumb = rs.getString("thumb") ;
                sml.visible = rs.getInt("visible") ;
                result.add(sml) ;

            }
            return result ;
        } catch (SQLException e) {
            System.out.println(e.getMessage()) ;
            return null ;
        }
    }

    public String rdbGetOffTaskJson(int oftid)   {
        try {
            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT oftid,scriptContent,extent,zmin,zmax,"
                    +" outProductId,outDatetime,startTime,endTime,"
                    +" uid,storage,stype,resultjson,path,htable,hpid,hcol "
                            + " ,hfami,hpidlen,hxylen,status "
                    +" FROM tbOfflineTask WHERE oftid="
                    +String.valueOf(oftid)+" LIMIT 1") ;
            if (rs.next()) {
                JOfflineTask offtask = new JOfflineTask();
                offtask.oftid = rs.getInt(1);
                offtask.scriptContent = rs.getString(2);
                offtask.extent = rs.getString(3);
                offtask.zmin = rs.getInt(4);
                offtask.zmax = rs.getInt(5);
                offtask.outProductId = rs.getInt(6);
                offtask.outDatetime = rs.getLong(7);
                offtask.startTime = rs.getLong(8);
                offtask.endTime = rs.getLong(9);
                offtask.uid = rs.getInt(10);
                offtask.storage = rs.getInt(11);
                offtask.stype = rs.getInt(12);
                offtask.resultjson = rs.getString(13);
                offtask.path = rs.getString(14);
                offtask.htable = rs.getString(15);
                offtask.hpid = rs.getInt(16);
                offtask.hcol = rs.getLong(17);
                offtask.hfami = rs.getString(18);
                offtask.hpidlen = rs.getInt(19);
                offtask.hxylen = rs.getInt(20);
                offtask.status = rs.getInt(21);

                Gson gson = new Gson();
                String jsontext = gson.toJson( offtask, JOfflineTask.class);
                return jsontext;
            }else{
                System.out.println("Error : not find offline task of "+ oftid);
                return null ;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage()) ;
            return null ;
        }
    }

    //deprecated 2021-3-27
    //public JProductInfo rdbGetProductInfoByMysqlPid(int mysqlPid)   {}



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
    //不再使用2021-4-21
    public int rdbNewOffTask1( String script, String uid,String path,String dt) {
        try
        {
//            String query = " insert into tbOfflineTask (scriptContent, outProductId, outDatetime,"
//                    +" startTime, uid, stype,"
//                    + "path, htable, hpid, "
//                    +" hcol, hfami, hpidlen, hxylen, status )"
//                    + " values (?,?,?, ?,?,?, ?,?,?, ?, ?,?,?,? )";
//            // create the mysql insert preparedstatement
//            PreparedStatement preparedStmt = JRDBHelperForWebservice.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
//            preparedStmt.setString (1, script);
//            preparedStmt.setInt (2, 0);
//            preparedStmt.setLong    (3, Long.parseLong(dt));
//            long starttime = this.getCurrentDatetime();
//            preparedStmt.setLong(4,starttime);
//            preparedStmt.setInt(5, Integer.parseInt(uid)) ;
//            preparedStmt.setInt(6,1);//script-type=1
//
//            preparedStmt.setString(7, path);
//            preparedStmt.setString( 8, WConfig.sharedConfig.hbaseuserfiletable) ;//hbase table name
//            preparedStmt.setInt( 9, 1);//hbase pid
//            preparedStmt.setLong( 10 , starttime);// hbase column name.
//
//            preparedStmt.setString(11 , "tiles");
//            preparedStmt.setInt(12,1);
//            preparedStmt.setInt(13,2);
//            preparedStmt.setInt(14,0);
//
//            preparedStmt.executeUpdate();
//            ResultSet rs = preparedStmt.getGeneratedKeys();
//            int last_inserted_id = -1 ;
//            if(rs.next())
//            {
//                last_inserted_id = rs.getInt(1);
//            }
            return -1;
        }catch (Exception ex )
        {
            System.out.println("Error : rdbNewRenderTask exception , " + ex.getMessage() ) ;
            return -1 ;
        }
    }

    public int rdbNewUserScript( int uid, String script0,int type){
        try
        {
            long dt0 = this.getCurrentDatetime();
            String query = " insert into tbscript (title, scriptcontent, updatetime, uid, type)"
                    + " values (?, ?, ?, ?, ?)";
            // create the mysql insert preparedstatement
            PreparedStatement preparedStmt = JRDBHelperForWebservice.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            preparedStmt.setString (1, "no-title");
            preparedStmt.setString (2, script0);
            preparedStmt.setLong   (3, dt0);
            preparedStmt.setInt    (4, uid);
            preparedStmt.setInt    (5, type);
            // execute the preparedstatement
            preparedStmt.executeUpdate();
            ResultSet rs = preparedStmt.getGeneratedKeys();
            int last_inserted_id = -1 ;
            if(rs.next())
            {
                last_inserted_id = rs.getInt(1);
                //update title
                String newtitle = "script-" + last_inserted_id;
                String query2 = "update tbscript set title = ? where sid = ?";
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

    //updated 2022-2-5 wf
    public String rdbGetUserScriptListJson(int uid)
    {
        try {
            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery("SELECT sid,title,updatetime,uid,type "
                    +" FROM tbscript WHERE uid="+uid+" LIMIT 200") ;
            Gson gson = new Gson();
            String outjson = "{\"scripts\":[" ;
            int nrec = 0 ;
            while (rs.next()) {
                JScript jscript = new JScript();
                jscript.sid = rs.getInt("sid");
                jscript.uid = rs.getInt("uid");
                jscript.title = rs.getString("title");
                jscript.scriptContent = "";
                jscript.utime = rs.getDate("utime");
                jscript.state = rs.getInt("state");
                jscript.jsfile = rs.getString("jsfile");

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


    //updated 2022-2-5 wf
    public JScript rdbGetUserScript(int sid)
    {
        try {
            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery("SELECT sid,title,scriptcontent,updatetime,uid,type "
                    +" FROM tbscript WHERE sid="+sid+" LIMIT 1") ;
            if (rs.next()) {
                JScript jscript = new JScript();
                jscript.sid = rs.getInt("sid");
                jscript.uid = rs.getInt("uid");
                jscript.title = rs.getString("title");
                jscript.scriptContent ="";
                jscript.utime = rs.getDate("utime");
                jscript.state = rs.getInt("state");
                jscript.jsfile = rs.getString("jsfile");
                return jscript;
            }
            return null;
        } catch (SQLException e) {
            System.out.println(e.getMessage()) ;
            return null ;
        }
    }

    //2022-2-5 wf
    public JScript rdbGetScript(int sid)
    {
        try {
            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * "
                    +" FROM tbscript WHERE sid="+sid ) ;
            if (rs.next()) {
                JScript jscript = new JScript();
                jscript.sid = rs.getInt("sid");
                jscript.uid = rs.getInt("uid");
                jscript.title = rs.getString("title");
                jscript.scriptContent = "";
                jscript.utime = rs.getDate("utime");
                jscript.state = rs.getInt("state");
                jscript.jsfile = rs.getString("jsfile") ;
                return jscript;
            }
            return null;
        } catch (SQLException e) {
            System.out.println(e.getMessage()) ;
            return null ;
        }
    }

    public JUser rdbGetUserByUname(String uname)
    {
        try {
            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery("SELECT uid,uname,password "
                    +" FROM tbuser WHERE uname='"+uname+"' LIMIT 1") ;
            if (rs.next()) {
                JUser user = new JUser();
                user.uid = rs.getInt("uid");
                user.uname = rs.getString("uname");
                user.password = rs.getString("password") ;
                return user;
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
                    String query2 = "update tbscript set title = ?, scriptcontent = ? , updatetime = ?  where sid = ?";
                    PreparedStatement preparedStmt2 = JRDBHelperForWebservice.getConnection().prepareStatement(query2);
                    preparedStmt2.setString   (1, title);
                    preparedStmt2.setString   (2, script);
                    preparedStmt2.setLong     (3, dt0);
                    preparedStmt2.setInt      (4, sid);
                    preparedStmt2.executeUpdate();
                }
                else if( script!=null )
                {
                    String query2 = "update tbscript set scriptcontent = ? , updatetime = ?  where sid = ?";
                    PreparedStatement preparedStmt2 = JRDBHelperForWebservice.getConnection().prepareStatement(query2);
                    preparedStmt2.setString   (1, script);
                    preparedStmt2.setLong     (2, dt0);
                    preparedStmt2.setInt      (3, sid);
                    preparedStmt2.executeUpdate();
                }else
                {
                    String query2 = "update tbscript set title = ? , updatetime = ?  where sid = ?";
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


    public String rdbGetUserPolyList(int uid)
    {
        try {
            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery("SELECT polyid,name,uid "
                    +" FROM tbPolygon WHERE uid="+uid+" LIMIT 200") ;
            Gson gson = new Gson();
            String outjson = "{\"results\":[" ;
            int nrec = 0 ;
            while (rs.next()) {
                JPePolygon poly = new JPePolygon();
                poly.polyid = rs.getInt("polyid");
                poly.name = rs.getString("name");
                poly.uid = rs.getInt("uid");

                String onejson = gson.toJson(poly, JPePolygon.class) ;
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
            String outjson = "{\"results\":[]}" ;
            return outjson ;
        }
    }

    public String rdbGetUserFileList(int uid)
    {
        try {
            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery("SELECT oftid,path,outProductId,hcol,uid,startTime "
                    +" FROM tbOfflineTask WHERE stype=1 AND uid="+uid+" LIMIT 200") ;
            Gson gson = new Gson();
            String outjson = "{\"results\":[" ;
            int nrec = 0 ;
            while (rs.next()) {
                JUserProductInfo res1 = new JUserProductInfo();
                res1.oftid = rs.getInt("oftid");
                res1.path = rs.getString("path");
                res1.outProductId = rs.getInt("outProductId");
                res1.hcol = rs.getLong("hcol");
                res1.uid = rs.getInt("uid");
                res1.startTime = rs.getLong("startTime");

                String onejson = gson.toJson(res1, JUserProductInfo.class) ;
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
            String outjson = "{\"results\":[]}" ;
            return outjson ;
        }
    }

    public String rdbGetPolyDetail(int polyid)
    {
        try {
            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery("SELECT polyid,name,geojson,uid "
                    +" FROM tbPolygon WHERE polyid="+polyid+" LIMIT 1") ;
            Gson gson = new Gson();
            if (rs.next()) {
                JPePolygon poly = new JPePolygon();
                poly.polyid = rs.getInt("polyid");
                poly.name = rs.getString("name");
                poly.geojson = rs.getString("geojson");
                poly.uid = rs.getInt("uid");

                String onejson = gson.toJson(poly, JPePolygon.class) ;
                return onejson;
            }
            return "{}";
        } catch (SQLException e) {
            return "{}" ;
        }
    }

    public String rdbGetOffTask1DetailByPath(String path)
    {
        try {
            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT oftid,scriptContent,extent,zmin,zmax,"
                            +" outProductId,outDatetime,startTime,endTime,"
                            +" uid,storage,stype,resultjson,path,htable,hpid,hcol "
                            +" FROM tbOfflineTask WHERE path='"
                            + path +"' LIMIT 1") ;
            if (rs.next()) {
                JOfflineTask offtask = new JOfflineTask();
                offtask.oftid = rs.getInt(1);
                offtask.scriptContent = rs.getString(2);
                offtask.extent = rs.getString(3);
                offtask.zmin = rs.getInt(4);
                offtask.zmax = rs.getInt(5);
                offtask.outProductId = rs.getInt(6);
                offtask.outDatetime = rs.getLong(7);
                offtask.startTime = rs.getLong(8);
                offtask.endTime = rs.getLong(9);
                offtask.uid = rs.getInt(10);
                offtask.storage = rs.getInt(11);
                offtask.stype = rs.getInt(12);
                offtask.resultjson = rs.getString(13);
                offtask.path = rs.getString(14);
                offtask.htable = rs.getString(15);
                offtask.hpid = rs.getInt(16);
                offtask.hcol = rs.getLong(17);

                Gson gson = new Gson();
                String jsontext = gson.toJson( offtask, JOfflineTask.class);
                return jsontext;
            }else{
                System.out.println("Warning : not find offline task by path:"+ path);
                return null ;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage()) ;
            return null ;
        }
    }

    public int rdbSavePoly(int uid,String geojson)
    {
        try
        {
            String query = " insert into tbPolygon (geojson,uid)"
                    + " values (?, ?)";
            // create the mysql insert preparedstatement
            PreparedStatement preparedStmt = JRDBHelperForWebservice.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            preparedStmt.setString (1, geojson);
            preparedStmt.setInt    (2, uid);
            // execute the preparedstatement
            preparedStmt.executeUpdate();
            ResultSet rs = preparedStmt.getGeneratedKeys();
            int last_inserted_id = -1 ;
            if(rs.next())
            {
                last_inserted_id = rs.getInt(1);
                //update name
                String polyname = "roi-" + last_inserted_id ;
                String query2 = "update tbPolygon set name = ? where polyid = ?";
                PreparedStatement preparedStmt2 = JRDBHelperForWebservice.getConnection().prepareStatement(query2);
                preparedStmt2.setString   (1, polyname);
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


    //2021-1-28
    public JPeStyle rdbGetStyle(int sid)
    {
        try {
            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery("SELECT styleContent "
                    +" FROM tbstyle WHERE styleid="+sid+" LIMIT 1") ;
            if (rs.next()) {
                String styleContent = rs.getString("styleContent") ;

                Gson g = new Gson() ;
                JPeStyle styleObj =  new Gson().fromJson(styleContent, JPeStyle.class) ;
                return styleObj;
            }
            return null;
        } catch (SQLException e) {
            System.out.println(e.getMessage()) ;
            return null ;
        }
    }

    //2021-1-28
    public String rdbGetStyleText(int sid)
    {
        try {
            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery("SELECT styleContent "
                    +" FROM tbstyle WHERE styleid="+sid+" LIMIT 1") ;
            if (rs.next()) {
                String styleContent = rs.getString("styleContent")  ;
                return styleContent;
            }
            return null;
        } catch (SQLException e) {
            System.out.println(e.getMessage()) ;
            return null ;
        }
    }

    //2021-3-23 获取一个简短的产品信息，包含HBase信息和波段信息
    public JProduct rdbGetProductForAPI(int pid )   {
        try{
            String productTable = "tbproduct" ;
            String bandTable = "tbproductband" ;

            JProduct result = new JProduct();
            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM "+productTable+" WHERE pid="+pid+" limit 1");
            if (rs.next()) {
                int uid = rs.getInt("userid");
                String name = rs.getString("name") ;
                String info = rs.getString("info");
                String source = rs.getString("source") ;
                JProduct pdt = new Gson().fromJson(info, JProduct.class) ;
                pdt.pid = pid ;
                pdt.userid = uid ;
                pdt.name = name ;
                pdt.source =source ;

                {//bandlist
                    Statement stmtb = JRDBHelperForWebservice.getConnection().createStatement();
                    ResultSet rsb = stmtb.executeQuery("SELECT * FROM "+bandTable+" WHERE pid="+String.valueOf(pid)
                            +" Order by bindex ASC") ;
                    while(rsb.next()){
                        int pidb = rsb.getInt("pid" );
                        int bindex = rsb.getInt("bIndex") ;
                        String infob = rsb.getString("info") ;
                        JProductBand band1 = new Gson().fromJson(infob, JProductBand.class) ;
                        band1.pid = pidb ;
                        band1.bIndex = bindex ;
                        pdt.bandList.add(band1) ;
                    }
                }

                {//HBase table
                    Statement stmth = JRDBHelperForWebservice.getConnection().createStatement();
                    ResultSet rsh = stmth.executeQuery("SELECT * FROM tbhbasetable WHERE htablename='"+
                            pdt.hTableName + "' LIMIT 1 ") ;
                    if( rsh.next() ){
                        pdt.hbaseTable.hTableName = rsh.getString("hTableName") ;
                        pdt.hbaseTable.hFamily = rsh.getString("hFamily") ;
                        pdt.hbaseTable.hPidByteNum = rsh.getInt("hPidByteNum") ;
                        pdt.hbaseTable.hYXByteNum = rsh.getInt("hYXByteNum") ;
                    }

                }

                result = pdt ;
            }
            return result ;
        }catch(Exception ex){
            System.out.println("rdbGetProductForAPI exception:"+ex.getMessage());
            return null ;
        }

    }


    //获取一个产品的显示信息
    public JProductDisplay rdbGetProductDisplayInfo(int pid)   {
        try{
            JProductDisplay pdt = new JProductDisplay();
            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rspd = stmt.executeQuery("SELECT * FROM tbproductdisplay WHERE pid="+pid+" limit 1");
            if (rspd.next()) {
                pdt.dpid = rspd.getInt("dpid") ;
                pdt.pid = rspd.getInt("pid") ;
                pdt.type = rspd.getString("type") ;
                pdt.satellite = rspd.getString("satellite") ;
                pdt.sensor = rspd.getString("sensor") ;
                pdt.productname = rspd.getString("productname") ;
                pdt.subtitle = rspd.getString("subtitle");
                pdt.productdescription = rspd.getString("productdescription") ;
                pdt.thumb = rspd.getString("thumb") ;
                pdt.visible = rspd.getInt("visible") ;
                pdt.cat = rspd.getInt("cat") ;
                pdt.iorder = rspd.getInt("iorder") ;
                pdt.params = rspd.getString("params") ;
            }
            return pdt ;
        }catch(Exception ex){
            System.out.println("rdbGetProductDisplayInfo exception:"+ex.getMessage());
            return null ;
        }
    }


    //获取一个渲染方案
    public JStyleDbObject rdbGetStyle2(int styleid)   {
        try{
            JStyleDbObject styleobj = new JStyleDbObject();
            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rspd = stmt.executeQuery("SELECT * FROM tbstyle WHERE styleid="+styleid+" limit 1");
            if (rspd.next()) {
                styleobj.styleid = rspd.getInt("styleid") ;
                styleobj.styleContent =  rspd.getString("styleContent") ;
                styleobj.description =  rspd.getString("description");
                styleobj.userid =  rspd.getInt("userid");
                styleobj.createtime =  rspd.getTime("createtime") ;
                styleobj.updatetime = rspd.getTime("updatetime");
            }
            return styleobj ;
        }catch(Exception ex){
            System.out.println("rdbGetProductDisplayInfo exception:"+ex.getMessage());
            return null ;
        }
    }



    //替换xyz产品的自定义变量
    // {{{DATE}}} {{{DATE-0}}} {{{DATE-1}}} {{{DATE-2}}}
    // {{{DATE-3}}} {{{DATE-4}}} {{{DATE-5}}} {{{DATE-6}}} {{{DATE-7}}}
    private String replaceCustomVariable(String ostr){
        String newstr = ostr ;
        DateFormat df = new SimpleDateFormat("yyyyMMdd") ;
        for(int iday = 0 ; iday <= 7 ; ++ iday )
        {
            String varname = "{{{DATE-" + String.valueOf(iday)+"}}}" ;
            Calendar cal = Calendar.getInstance() ;
            cal.add(Calendar.DATE,-iday) ;
            Date date = cal.getTime() ;
            String datestr = df.format(date) ;
            if( iday==0 ){
                newstr = newstr.replace("{{{DATE}}}" , datestr) ;
            }
            newstr = newstr.replace(varname , datestr) ;
        }
        return newstr ;
    }

    //获取一个产品的显示信息by displayid
    public JProductDisplay rdbGetProductDisplayInfoByDisplayId(int displayid)   {
        try{
            JProductDisplay pdt = new JProductDisplay();
            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rspd = stmt.executeQuery("SELECT * FROM tbproductdisplay WHERE dpid="+displayid+" limit 1");
            if (rspd.next()) {
                pdt.dpid = rspd.getInt("dpid") ;
                pdt.pid = rspd.getInt("pid") ;
                pdt.type = rspd.getString("type") ;
                pdt.satellite = rspd.getString("satellite") ;
                pdt.sensor = rspd.getString("sensor") ;
                pdt.productname = rspd.getString("productname") ;
                pdt.subtitle = rspd.getString("subtitle");
                pdt.productdescription = rspd.getString("productdescription") ;
                pdt.thumb = rspd.getString("thumb") ;
                pdt.visible = rspd.getInt("visible") ;
                pdt.cat = rspd.getInt("cat") ;
                pdt.iorder = rspd.getInt("iorder") ;
                pdt.params = rspd.getString("params") ;

                //xyz 图层增加自定义变量的替换，目前只支持一个变量
                if( pdt.type.compareTo("xyz")==0 )
                {
                    pdt.productname = replaceCustomVariable(pdt.productname) ;
                    pdt.params = replaceCustomVariable(pdt.params) ;
                }
            }
            return pdt ;
        }catch(Exception ex){
            System.out.println("rdbGetProductDisplayInfo exception:"+ex.getMessage());
            return null ;
        }
    }


    //2021-3-23 显示全部系统产品，不包括用户产品
    public ArrayList<JProduct> rdbGetProducts() throws SQLException {
        ArrayList<JProduct> result = new ArrayList<>() ;
        Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM tbproduct WHERE userid=0 ") ;
        while (rs.next()) {
            int pid = rs.getInt("pid");
            int uid = rs.getInt("userid");
            String name = rs.getString("name") ;
            String info = rs.getString("info");
            String source = rs.getString("source") ;
            JProduct pdt = new Gson().fromJson(info, JProduct.class) ;
            pdt.pid = pid ;
            pdt.userid = uid ;
            pdt.name = name ;
            pdt.source = source ;
            pdt.caps = new String[]{"zs","xl","co","ex","st","dt"} ;
            {//bandlist
                Statement stmtb = JRDBHelperForWebservice.getConnection().createStatement();
                ResultSet rsb = stmtb.executeQuery("SELECT * FROM tbproductband WHERE pid="+String.valueOf(pid)
                        +" Order by bindex ASC") ;
                while(rsb.next()){
                    int pidb = rsb.getInt("pid" );
                    int bindex = rsb.getInt("bIndex") ;
                    String infob = rsb.getString("info") ;
                    JProductBand band1 = new Gson().fromJson(infob, JProductBand.class) ;
                    band1.pid = pidb ;
                    band1.bIndex = bindex ;
                    pdt.bandList.add(band1) ;
                }
            }

            {//HBase table
                Statement stmth = JRDBHelperForWebservice.getConnection().createStatement();
                ResultSet rsh = stmth.executeQuery("SELECT * FROM tbhbasetable WHERE htablename='"+
                        pdt.hTableName + "' LIMIT 1 ") ;
                if( rsh.next() ){
                    pdt.hbaseTable.hTableName = rsh.getString("hTableName") ;
                    pdt.hbaseTable.hFamily = rsh.getString("hFamily") ;
                    pdt.hbaseTable.hPidByteNum = rsh.getInt("hPidByteNum") ;
                    pdt.hbaseTable.hYXByteNum = rsh.getInt("hYXByteNum") ;
                }

            }

            {//product display info
                pdt.productDisplay = this.rdbGetProductDisplayInfo(pdt.pid) ;
            }

            {//latest datetime
                Statement stmtdt = JRDBHelperForWebservice.getConnection().createStatement();
                ResultSet rsdt= stmtdt.executeQuery("SELECT * FROM tbproductdataitem WHERE pid='"+
                        pdt.pid + "' Order by hcol DESC LIMIT 1 ") ;
                if( rsdt.next() ){
                    pdt.latestDataItem.fid = rsdt.getInt("fid") ;
                    pdt.latestDataItem.pid = rsdt.getInt("pid") ;
                    pdt.latestDataItem.hcol = rsdt.getLong("hcol") ;
                    pdt.latestDataItem.convertShowValRealVal(pdt.timeType);
                }
            }
            result.add(pdt) ;
        }
        return result ;
    }

    //2021-4-29获取一个完整的可加载到图层的产品信息
    public JProduct rdbGetOneProductLayerInfoById(int mysqlPid )  {
        try {
            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            String productTable = "tbproduct" ;
            String bandTable = "tbproductband" ;
            String dataItemTable = "tbproductdataitem" ;

            String tsql = "SELECT * FROM "+productTable+" WHERE pid=" + mysqlPid + " limit 1" ;
            ResultSet rs = stmt.executeQuery(tsql) ;
            if (rs.next()) {
                int pid = rs.getInt("pid");
                int uid = rs.getInt("userid");
                String name = rs.getString("name") ;
                String info = rs.getString("info");
                String source = rs.getString("source") ;
                JProduct pdt = new Gson().fromJson(info, JProduct.class) ;
                pdt.pid = pid ;
                pdt.userid = uid ;
                pdt.name = name ;
                pdt.source = source ;
                {//bandlist
                    Statement stmtb = JRDBHelperForWebservice.getConnection().createStatement();
                    ResultSet rsb = stmtb.executeQuery("SELECT * FROM "+bandTable+" WHERE pid="+String.valueOf(pid)
                            +" Order by bindex ASC") ;
                    while(rsb.next()){
                        int pidb = rsb.getInt("pid" );
                        int bindex = rsb.getInt("bIndex") ;
                        String infob = rsb.getString("info") ;
                        JProductBand band1 = new Gson().fromJson(infob, JProductBand.class) ;
                        band1.pid = pidb ;
                        band1.bIndex = bindex ;
                        pdt.bandList.add(band1) ;
                    }
                }

                {//HBase table
                    Statement stmth = JRDBHelperForWebservice.getConnection().createStatement();
                    ResultSet rsh = stmth.executeQuery("SELECT * FROM tbhbasetable WHERE htablename='"+
                            pdt.hTableName + "' LIMIT 1 ") ;
                    if( rsh.next() ){
                        pdt.hbaseTable.hTableName = rsh.getString("hTableName") ;
                        pdt.hbaseTable.hFamily = rsh.getString("hFamily") ;
                        pdt.hbaseTable.hPidByteNum = rsh.getInt("hPidByteNum") ;
                        pdt.hbaseTable.hYXByteNum = rsh.getInt("hYXByteNum") ;
                    }
                }

                {//product display info
                    pdt.productDisplay = this.rdbGetProductDisplayInfo(pdt.pid) ;
                    if( pdt.productDisplay==null ){
                        pdt.productDisplay = new JProductDisplay() ;
                    }
                }

                {//latest datetime
                    Statement stmtdt = JRDBHelperForWebservice.getConnection().createStatement();
                    ResultSet rsdt= stmtdt.executeQuery("SELECT * FROM "+dataItemTable+" WHERE pid='"+
                            pdt.pid + "' Order by hcol DESC LIMIT 1 ") ;
                    if( rsdt.next() ){
                        pdt.latestDataItem.fid = rsdt.getInt("fid") ;
                        pdt.latestDataItem.pid = rsdt.getInt("pid") ;
                        pdt.latestDataItem.hcol = rsdt.getLong("hcol") ;
                        pdt.latestDataItem.convertShowValRealVal(pdt.timeType);
                    }
                }
                return pdt ;
            }
            else{
                return null ;
            }
        }catch (Exception ex)
        {
            System.out.println("rdbGetOneProductLayerInfoById exception:" + ex.getMessage());
            return null ;
        }
    }

    //获取系统产品期次信息
    public ArrayList<JProductDataItem> rdbGetProductDataItemList(int pid,
                                                                 int ipage,
                                                                 int pagesize,
                                                                 String orderstr ) throws SQLException {
        JProduct pdt = rdbGetProductForAPI(pid) ;

        ArrayList<JProductDataItem> result = new ArrayList<>();
        Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
        String sqlstr = String.format("SELECT * FROM tbproductdataitem WHERE pid=%d Order by hcol %s LIMIT %d,%d ",
                pid,orderstr,ipage*pagesize,pagesize) ;
        ResultSet rs = stmt.executeQuery(sqlstr );
        while (rs.next()) {
            JProductDataItem di = new JProductDataItem() ;
            di.fid = rs.getInt("fid");
            di.hcol = rs.getLong("hcol") ;
            di.convertShowValRealVal(pdt.timeType);
            result.add(di) ;
        }
        return result ;
    }

    public ArrayList<Integer> rdbGetProductYearList(int pid)
            throws SQLException {
        JProduct pdt = rdbGetProductForAPI(pid) ;
        ArrayList<Integer> result = new ArrayList<>();
        Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
        String sqlstr = String.format("SELECT DISTINCT FLOOR(hcol/10000000000) as year FROM tbproductdataitem "
                +"WHERE pid=%d Order by year ASC",
                pid) ;
        ResultSet rs = stmt.executeQuery(sqlstr );
        while (rs.next()) {
            Integer year1 = rs.getInt("year");
            result.add(year1) ;
        }
        return result ;
    }

    public ArrayList<Integer> rdbGetProductMonthList(int pid,
                                                     int year) throws SQLException {
        JProduct pdt = rdbGetProductForAPI(pid) ;
        ArrayList<Integer> result = new ArrayList<>();
        Long ymd0 = year*10000000000L ;
        Long ymd1 = (year+1)*10000000000L ;
        Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
        String sqlstr = String.format("SELECT DISTINCT FLOOR(hcol/100000000) as yearmon FROM tbproductdataitem "
                        +"WHERE pid=%d AND hcol>=%d AND hcol<%d Order by yearmon ASC",
                pid,ymd0,ymd1 ) ;
        ResultSet rs = stmt.executeQuery(sqlstr );
        while (rs.next()) {
            Integer tmon = rs.getInt("yearmon") % 100 ;
            result.add(tmon) ;
        }
        return result ;
    }

    public ArrayList<JProductDataItem> rdbGetProductMonthDataItemList(
            int pid,
            int year,
            int mon
            ) throws SQLException {
        JProduct pdt = rdbGetProductForAPI(pid) ;
        ArrayList<JProductDataItem> result = new ArrayList<>();
        Long ymd0 = (year*10000L+mon*100L    )*1000000L ;
        Long ymd1 = (year*10000L+(mon+1)*100L)*1000000L ;
        Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
        String sqlstr = String.format("SELECT * FROM tbproductdataitem "
                        +"WHERE pid=%d AND hcol>=%d AND hcol<%d Order by hcol ASC",
                pid,ymd0,ymd1 ) ;
        ResultSet rs = stmt.executeQuery(sqlstr );
        while (rs.next()) {
            JProductDataItem di = new JProductDataItem() ;
            di.fid = rs.getInt("fid");
            di.hcol = rs.getLong("hcol") ;
            di.convertShowValRealVal(pdt.timeType);
            result.add(di) ;
        }
        return result ;
    }

    //输入hcol，在dataitem表中找到小于等于hcol的最近的值
    public JProductDataItem rdbGetLowerEqualNearestHCol(int pid,Long hcol,int timeType)
    {
        try{
            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            String sqlstr = String.format("SELECT * FROM tbproductdataitem "
                            +"WHERE pid=%d AND hcol<=%d Order by hcol DESC LIMIT 1",
                    pid,hcol ) ;
            ResultSet rs = stmt.executeQuery(sqlstr );
            if (rs.next()){
                JProductDataItem di = new JProductDataItem() ;
                di.fid = rs.getInt("fid");
                di.pid = rs.getInt("pid") ;
                di.hcol = rs.getLong("hcol") ;
                di.convertShowValRealVal(timeType);
                return di;
            }else{
                return null;
            }
        }catch (SQLException ex){
            return null;
        }
    }

    //通过父节点编码查找子节点的数组
    public ArrayList<Area> rdbGetAreaList(String parentCode) throws SQLException {
        ArrayList<Area> result = new ArrayList<>();
        Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
        String sqlstr = String.format("SELECT * FROM area WHERE parent_code='%s' ",parentCode) ;
        ResultSet rs = stmt.executeQuery(sqlstr );
        while (rs.next()) {
            Area aa = new Area() ;
            aa.id = rs.getInt("id") ;
            aa.code = rs.getString("code") ;
            aa.name = rs.getString("name") ;
            aa.parentCode = rs.getString("parent_code") ;
            aa.path = rs.getString("path") ;
            aa.children = this.rdbAreaHasChildren(aa.code) ;
            result.add(aa) ;
        }
        return result ;
    }



    //通过ID获取一个对象
    public Area rdbGetArea(int theid) throws SQLException {
        Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
        String sqlstr = String.format("SELECT * FROM area WHERE id='%s' ",theid) ;
        ResultSet rs = stmt.executeQuery(sqlstr );
        if (rs.next()) {
            Area aa = new Area() ;
            aa.id = rs.getInt("id") ;
            aa.code = rs.getString("code") ;
            aa.name = rs.getString("name") ;
            aa.parentCode = rs.getString("parent_code") ;
            aa.path = rs.getString("path") ;
            aa.children = this.rdbAreaHasChildren(aa.code) ;
            return aa ;
        }else
        {
            return null ;
        }
    }

    //通过ID获取一个Region对象
    //will not update 2022-2-13
    public JRegionDbObject rdbGetRegion(int theid) throws SQLException {
        Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
        String sqlstr = String.format("SELECT * FROM tbregion WHERE rid='%s' ",theid) ;
        ResultSet rs = stmt.executeQuery(sqlstr );
        if (rs.next()) {
            JRegionDbObject result = new JRegionDbObject() ;
            result.rid =  rs.getInt("rid") ;
            result.name =  rs.getString("name");
            result.shp =  rs.getString("shp") ;
            result.geojson =  rs.getString("geojson") ;
            result.uid =  rs.getInt("uid") ;
            return result ;
        }else
        {
            return null ;
        }
    }



    public int rdbAreaHasChildren(String parentCode)  {
        try{
            ArrayList<Area> result = new ArrayList<>();
            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            String sqlstr = String.format("SELECT `id` FROM area WHERE parent_code='%s' LIMIT 1",parentCode) ;
            ResultSet rs = stmt.executeQuery(sqlstr );
            if( rs.next() ){
                return 1 ;
            }else{
                return 0 ;
            }
        }catch(Exception ex){
            System.out.println("rdbAreaHasChildren exception:"+ex.getMessage());
            return 0 ;
        }
    }

    //获取感兴趣区或者行政区的geojson路径
    //will not updated 2022-2-13
    public String rdbGetGeoJsonFilePath(String rtype,Long rid) {
        try{
            String sqlstr = "" ;
            if( rtype.equals("area")==true ){
                sqlstr = String.format("SELECT path FROM area WHERE id=%d ", rid) ;
            }else{
                sqlstr = String.format("SELECT geojson FROM tbregion WHERE rid=%d ",rid) ;
            }
            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sqlstr );
            if (rs.next()) {
                String result = rs.getString(1) ;
                // result = WConfig.sharedConfig.pedir + result ; //removed 2022-2-13
                return result ;
            }else{
                System.out.println("rdbGetGeoJsonFilePath no record for "+rtype+","+rid);
                return null ;
            }
        }catch(Exception ex){
            System.out.println("rdbGetGeoJsonFilePath exception:"+ex.getMessage());
            return null ;
        }
    }

    //获取感兴趣区或者行政区的信息
    public ROI rdbGetROIInfo(String rtype,int rid) {
        try{
            if( rtype.equals("area")==true ){
                Area aa = this.rdbGetArea(rid) ;
                return ROI.convertArea2ROI(aa) ;
            }else{
                JRegionDbObject rr = this.rdbGetRegion(rid) ;
                return ROI.convertRegionDTO2ROI(rr) ;
            }
        }catch(Exception ex){
            System.out.println("rdbGetROIInfo exception:"+ex.getMessage());
            return null ;
        }

    }

    //新建一个空的产品记录，用于数据合成或者其他离线任务，返回的主键ID用于hbase中的hpid
    //返回主键id
    public int rdbNewEmptyUserProduct( String name, int uid) {
        try
        {
            String query = " insert into tbproduct (name, userid, source)"
                    + " values (?, ?, ?)";
            // create the mysql insert preparedstatement
            PreparedStatement preparedStmt = JRDBHelperForWebservice.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            preparedStmt.setString (1, name);
            preparedStmt.setInt    (2, uid);
            preparedStmt.setString (3, "hbase");
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
            System.out.println("Error : rdbNewEmptyProduct exception , " + ex.getMessage() ) ;
            return -1 ;
        }

    }

    //获取用户预加载产品dpid列表，如果没有用户记录返回系统记录，如果有多条记录返回最新一条 2021-5-29
    public JPreloadMapsData rdbGetPreloadMapsDisplayId( int uid){
        JPreloadMapsData outdata = new JPreloadMapsData() ;
        try{
            String sqlstr = String.format("SELECT * FROM tbpreloadmap WHERE uid=%d Order by premapid DESC LIMIT 1",uid)  ;
            Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sqlstr );
            if (rs.next()) {
                outdata.premapid = rs.getInt(1) ;
                outdata.uid = rs.getInt(2) ;
                outdata.preloadlist = rs.getString(3) ;
                return outdata ;
            }else{
                sqlstr = String.format("SELECT * FROM tbpreloadmap WHERE uid=0 Order by premapid DESC LIMIT 1")  ;
                Statement stmt2 = JRDBHelperForWebservice.getConnection().createStatement();
                ResultSet rs2 = stmt2.executeQuery(sqlstr );
                if (rs2.next()) {
                    outdata.premapid = rs2.getInt(1) ;
                    outdata.uid = rs2.getInt(2) ;
                    outdata.preloadlist = rs2.getString(3) ;
                    return outdata ;
                }else {
                    System.out.println("rdbGetPreloadMapsDisplayId no record");
                    return null ;
                }
            }
        }catch(Exception ex){
            System.out.println("rdbGetPreloadMapsDisplayId exception:"+ex.getMessage());
            return null ;
        }
    }

    //insert user preloadlist
    public boolean rdbInsertPreloadlist( int uid , String preloadlist ){
        try
        {
            String query = " insert into tbpreloadmap (uid, preloadlist)"
                    + " values (?, ?)";
            // create the mysql insert preparedstatement
            PreparedStatement preparedStmt = JRDBHelperForWebservice.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            preparedStmt.setInt (1, uid);
            preparedStmt.setString(2, preloadlist);
            // execute the preparedstatement
            preparedStmt.executeUpdate();
            ResultSet rs = preparedStmt.getGeneratedKeys();
            int last_inserted_id = -1 ;
            if(rs.next())
            {
                last_inserted_id = rs.getInt(1);
            }
            if( last_inserted_id>0){
                return true ;
            }else{
                return false ;
            }
        }catch (Exception ex )
        {
            System.out.println("Error : rdbInsertPreloadlist exception , " + ex.getMessage() ) ;
            return false ;
        }
    }

    //update user preloadlist
    public boolean rdbUpdatePreloadlist( int premapid , String preloadlist ){
        try{
            //update
            String query2 = "update tbpreloadmap set preloadlist = ? where premapid = ?";
            PreparedStatement preparedStmt2 = JRDBHelperForWebservice.getConnection().prepareStatement(query2);
            preparedStmt2.setString   (1, preloadlist);
            preparedStmt2.setInt      (2, premapid);
            preparedStmt2.executeUpdate();
            return true ;
        }catch (Exception ex )
        {
            System.out.println("Error : rdbUpdatePreloadlist exception , " + ex.getMessage() ) ;
            return false ;
        }
    }

    //delete user preloadlist
    public boolean rdbDeletePreloadlist( int uid  ){
        try{
            //update  DELETE FROM table_name [WHERE Clause]
            String query2 = "DELETE FROM tbpreloadmap where uid = ?";
            PreparedStatement preparedStmt2 = JRDBHelperForWebservice.getConnection().prepareStatement(query2);
            preparedStmt2.setInt      (1, uid);
            preparedStmt2.executeUpdate();
            return true ;
        }catch (Exception ex )
        {
            System.out.println("Error : rdbDeletePreloadlist exception , " + ex.getMessage() ) ;
            return false ;
        }
    }

    //get all visible display product list 2021-6-21
    public ArrayList<Integer> rdbGetAllDisplayProduct(){
        try{
            //
            String query2 = "SELECT dpid FROM tbproductdisplay WHERE visible=1 Order by iorder ASC";
            Statement stmt2 = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rs = stmt2.executeQuery(query2 );
            ArrayList<Integer> displayProductIdArr = new ArrayList<Integer>() ;
            while (rs.next()) {
                int dpid = rs.getInt(1) ;
                displayProductIdArr.add(dpid) ;
            }
            return displayProductIdArr ;
        }catch (Exception ex )
        {
            System.out.println("Error : rdbGetAllDisplayProduct exception , " + ex.getMessage() ) ;
            return null ;
        }
    }


    //获取全部系统Roi分类
    public ArrayList<JRoiCategory> rdbGetSysRoiCategories() {
        try{
            //
            String query2 = "SELECT * FROM tbroicat WHERE visible=1 Order by iorder ASC";
            Statement stmt2 = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rs = stmt2.executeQuery(query2 );
            ArrayList<JRoiCategory> res = new ArrayList<>() ;
            while (rs.next()) {
                JRoiCategory rcat = new JRoiCategory();
                rcat.rcid = rs.getInt(1) ;
                rcat.name = rs.getString(2) ;
                rcat.iorder = rs.getInt(3);
                rcat.visible = rs.getInt(4) ;
                res.add(rcat);
            }
            return res ;
        }catch (Exception ex )
        {
            System.out.println("Error : rdbGetSysRoiCategories exception , " + ex.getMessage() ) ;
            return null ;
        }
    }

    //分页获取系统ROI
    public ArrayList<JRoi2> rdbGetSysRoiItemes(int catid,int offset) {
        try{
            //
            String query2 = "SELECT * FROM tbroisys WHERE rcid="+String.valueOf(catid)+" Order by rid ASC LIMIT "+String.valueOf(offset)+",20";
            Statement stmt2 = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rs = stmt2.executeQuery(query2 );
            ArrayList<JRoi2> res = new ArrayList<>() ;
            while (rs.next()) {
                JRoi2 roi2 = new JRoi2();
                roi2.rid = rs.getInt(1) ;
                roi2.rcid = rs.getInt(2);
                roi2.name = rs.getString(3);
                roi2.name2 = rs.getString(4);
                roi2.geojson = rs.getString(5) ;
                res.add(roi2);
            }
            return res ;
        }catch (Exception ex )
        {
            System.out.println("Error : rdbGetSysRoiItemes exception , " + ex.getMessage() ) ;
            return null ;
        }
    }

    //获取系统ROI数量
    public int rdbGetSysRoiItemesCount(int catid) {
        try{
            //
            String query2 = "SELECT count(rid) FROM tbroisys WHERE rcid="+String.valueOf(catid) ;
            Statement stmt2 = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rs = stmt2.executeQuery(query2 );
            int count = 0 ;
            if( rs.next() ){
                count = rs.getInt(1);
            }
            return count ;
        }catch (Exception ex )
        {
            System.out.println("Error : rdbGetSysRoiItemes exception , " + ex.getMessage() ) ;
            return 0 ;
        }
    }

    //分页获取User - ROI
    public ArrayList<JRoi2> rdbGetUserRoiItemes(int uid) {
        try{
            //
            String query2 = "SELECT * FROM tbroiuser WHERE uid="+String.valueOf(uid)+" Order by rid ASC ";
            Statement stmt2 = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rs = stmt2.executeQuery(query2 );
            ArrayList<JRoi2> res = new ArrayList<>() ;
            while (rs.next()) {
                JRoi2 roi2 = new JRoi2();
                roi2.rid = rs.getInt(1) ;
                roi2.name = rs.getString(2);
                roi2.shp = rs.getString(3) ;
                roi2.geojson = rs.getString(4) ;
                roi2.uid = uid ;//column 5
                roi2.ctime = rs.getDate(6) ;
                res.add(roi2);
            }
            return res ;
        }catch (Exception ex )
        {
            System.out.println("Error : rdbGetUserRoiItemes exception , " + ex.getMessage() ) ;
            return null ;
        }
    }

    // 获取User - ROI
    public JRoi2 rdbGetUserRoiItem(int rid) {
        try{
            //
            String query2 = "SELECT * FROM tbroiuser WHERE rid="+String.valueOf(rid) ;
            Statement stmt2 = JRDBHelperForWebservice.getConnection().createStatement();
            ResultSet rs = stmt2.executeQuery(query2 );
            if (rs.next()) {
                JRoi2 roi2 = new JRoi2();
                roi2.rid = rs.getInt(1) ;
                roi2.name = rs.getString(2);
                roi2.shp = rs.getString(3) ;
                roi2.geojson = rs.getString(4) ;
                roi2.uid = rs.getInt(5) ;//column 5
                roi2.ctime = rs.getDate(6) ;
                return  roi2;
            }
            return null ;
        }catch (Exception ex )
        {
            System.out.println("Error : rdbGetUserRoiItem exception , " + ex.getMessage() ) ;
            return null ;
        }
    }


    //感兴趣区入库 insert
    public int rdbNewRoi2( String name, String shpRelPath, String geojsonRelPath , int uid) {
        try
        {
            String query = " insert into tbroiuser (name,shp,geojson,uid,ctime)"
                    + " values (?, ?, ?, ?, ?)";
            // create the mysql insert preparedstatement
            PreparedStatement preparedStmt = JRDBHelperForWebservice.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            preparedStmt.setString (1, name );
            preparedStmt.setString (2, shpRelPath);
            preparedStmt.setString   (3, geojsonRelPath);
            preparedStmt.setInt    (4, uid);
            preparedStmt.setString    (5, getCurrentDatetimeStr() );
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
            System.out.println("Error : rdbNewRoi2 exception , " + ex.getMessage() ) ;
            return -1 ;
        }
    }



    //remove roi from tbroiuser
    public boolean rdbRemoveUserRoi(int rid) {
        try
        {
            String query = "delete from tbroiuser where rid=?" ;
            // create the mysql insert preparedstatement
            PreparedStatement preparedStmt = JRDBHelperForWebservice.getConnection().prepareStatement(query);
            preparedStmt.setInt      (1, rid);
            preparedStmt.executeUpdate();
            return true ;
        }catch (Exception ex )
        {
            System.out.println("Error : rdbRemoveUserRoi exception , " + ex.getMessage() ) ;
            return false ;
        }
    }



    //将订单信息写入tbofftask 2022-2-8
    public int rdbNewOffTask(int uid,int mode,String orderRelFilePath,
                             String resultRelFilePath ) {
        try
        {
            String query = " insert into tbofftask (mode,uid,orderfile,resultfile,ctime,utime,status,tag,msg)"
                    + " values (?, ?, ?,   ?, ?, ? ,  ? ,? ,?)";
            // create the mysql insert preparedstatement
            PreparedStatement preparedStmt = JRDBHelperForWebservice.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            preparedStmt.setInt (1, mode );
            preparedStmt.setInt (2, uid);
            preparedStmt.setString   (3, orderRelFilePath);
            preparedStmt.setString    (4, resultRelFilePath);
            preparedStmt.setString    (5, getCurrentDatetimeStr() );//ctime
            preparedStmt.setString    (6, getCurrentDatetimeStr() );//utime
            preparedStmt.setInt    (7, 0 );//
            preparedStmt.setString    (8, "" );//
            preparedStmt.setString    (9, "");//
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
            System.out.println("Error : rdbNewOffTask exception , " + ex.getMessage() ) ;
            return -1 ;
        }
    }

    public boolean updateOfftaskByWorkerResult ( JOfftaskWorkerResult workerRes){
        try{
            //
            int status = 3 ;//0-not start; 1-running; 2-done; 3-failed.
            if(workerRes.state==0) status = 2 ;
            String query2 = "UPDATE tbofftask SET utime=? , status=? WHERE ofid=?";
            PreparedStatement preparedStmt2 = JRDBHelperForWebservice.getConnection().prepareStatement(query2);
            preparedStmt2.setString(1 , getCurrentDatetimeStr());
            preparedStmt2.setInt      (2, status);
            preparedStmt2.setInt      (3, workerRes.ofid);
            preparedStmt2.executeUpdate();
            return true ;
        }catch (Exception ex )
        {
            System.out.println("Error : updateOfftaskByWorkerResult exception , " + ex.getMessage() ) ;
            return false ;
        }
    }


    /** 2022-3-24
     * 对已有产品添加波段记录，不做重复性检验，
     * @param mypid
     * @param hpid
     * @param numbands
     * @return
     */
    public boolean writeProductBandRecord(int mypid, int hpid,int numbands,double validmin,double validmax,double filldata)
    {
        //        {
        //            "hPid":1,
        //                "bsqIndex":0,
        //                "bName":"NDVI",
        //                "scale":0.0001,
        //                "offset":0,
        //                "validMin":-2000,
        //                "validMax":12000,
        //                "noData":-3000
        //        }
        try
        {
            String allquery = "" ;
            for(int ib = 0 ;ib<numbands;++ib )
            {
                String bandinfo =
                        "{\"hPid\":" + hpid + ","
                                +"\"bsqIndex\":"+ib+","
                                +"\"bName\":"+"\"B"+(ib+1)+"\","
                                +"\"scale\":1,"
                                +"\"offset\":0,"
                                +"\"validMin\":"+validmin+","
                                +"\"validMax\":"+validmax+","
                                +"\"noData\":"+filldata
                                +"}" ;
                String query = "INSERT INTO `tbproductband`(`pid`, `bindex`, `info`) VALUES ("+mypid+","+ib+",'"+bandinfo+"');";
                allquery += query ;
            }

            // create the mysql insert preparedstatement
            PreparedStatement preparedStmt = JRDBHelperForWebservice.getConnection().prepareStatement(allquery);
            // execute the preparedstatement
            preparedStmt.executeUpdate();
            int num = preparedStmt.getUpdateCount() ;
            if( num>0 ){
                System.out.println("writeProductBandRecord update count:"+num) ;
                return true ;
            }else{
                return false ;
            }
        }catch (Exception ex )
        {
            System.out.println("Error : writeProductBandRecord exception , " + ex.getMessage() ) ;
            return false ;
        }
    }


    /** 2022-3-24
     * 写入一条产品记录
     * @param mypid
     * @param hcol
     * @param left
     * @param right
     * @param top
     * @param bottom
     * @return
     */
    public int writeProductDataItem(int mypid,long hcol,double left,double right,double top,double bottom)
    {
        try
        {
            //INSERT INTO `tbproductdataitem`(`fid`, `pid`, `hcol`, `hleft`, `hright`, `htop`, `hbottom`, `createtime`, `updatetime`) VALUES ('[value-1]','[value-2]','[value-3]','[value-4]','[value-5]','[value-6]','[value-7]','[value-8]','[value-9]')
            String query = "INSERT INTO `tbproductdataitem`(`pid`, `hcol`, `hleft`, `hright`, `htop`, `hbottom`, `createtime`, `updatetime`)"
                    + " VALUES (?, ?,  ?,?,?,?,   ?,?)";
            // create the mysql insert preparedstatement
            PreparedStatement preparedStmt = JRDBHelperForWebservice.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            preparedStmt.setInt (1, mypid );
            preparedStmt.setLong (2, hcol);
            preparedStmt.setDouble   (3, left);
            preparedStmt.setDouble    (4, right);
            preparedStmt.setDouble    (5, top );
            preparedStmt.setDouble    (6, bottom );
            preparedStmt.setString    (7, getCurrentDatetimeStr() );//
            preparedStmt.setString    (8, getCurrentDatetimeStr());//
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
            System.out.println("Error : writeProductDataItem exception , " + ex.getMessage() ) ;
            return -1 ;
        }
    }


    /** 2022-3-24
     * 更新产品信息
     * @param mypid
     * @param name
     * @param proj
     * @param minZoom
     * @param maxZoom
     * @param dataType
     * @param timeType 时间尺度，1秒，2分钟，3小时，4日、5月、6季、7年，后面还可以包括11（候）、12（八天）、13（旬）、14（16天）。用户生产的文件没有时间尺度意义，使用0值。
     * @param hTableName
     * @param tileWid
     * @param tileHei
     * @param compress
     * @param styleid
     * @return
     */
    public boolean updateProductNameAndInfo(int mypid,
                                            String name,
                                            String proj,
                                            Integer minZoom,Integer maxZoom,
                                            Integer dataType,
                                            Integer timeType,
                                            String hTableName,
                                            Integer tileWid,
                                            Integer tileHei,
                                            String compress,
                                            Integer styleid ){
        //        {
        //            "proj":"EPSG:4326",
        //                "minZoom":0,
        //                "maxZoom":5,
        //                "dataType":3,
        //                "timeType":5,
        //                "hTableName":"sparkv8out",
        //                "tileWid":256,
        //                "tileHei":256,
        //                "compress":"deflate",
        //                "styleid":0
        //        }
        Gson gson = new Gson() ;
        Map<String, Object> pdtinfo = new HashMap<>();
        pdtinfo.put("proj", proj);
        pdtinfo.put("minZoom", minZoom);
        pdtinfo.put("maxZoom", maxZoom);
        pdtinfo.put("dataType", dataType);
        pdtinfo.put("timeType", timeType);
        pdtinfo.put("hTableName", hTableName);
        pdtinfo.put("tileWid", tileWid);
        pdtinfo.put("tileHei", tileHei);
        pdtinfo.put("compress", compress);
        pdtinfo.put("styleid", styleid);

        String pdtinfojsonstr = gson.toJson(pdtinfo) ;

        try{
            //
            String query2 = "UPDATE tbproduct SET name=? , info=? WHERE pid=?";
            PreparedStatement preparedStmt2 = JRDBHelperForWebservice.getConnection().prepareStatement(query2);
            preparedStmt2.setString(1 , name);
            preparedStmt2.setString(2, pdtinfojsonstr);
            preparedStmt2.setInt   (3, mypid);
            preparedStmt2.executeUpdate();
            return true ;
        }catch (Exception ex )
        {
            System.out.println("Error : updateProductNameAndInfo exception , " + ex.getMessage() ) ;
            return false ;
        }
    }

}
