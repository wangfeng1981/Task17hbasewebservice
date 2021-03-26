package com.pixelengine;

import com.google.gson.Gson;
import com.pixelengine.DataModel.Area;
import com.pixelengine.DataModel.JProduct;
import com.pixelengine.DataModel.JProductBand;
import com.pixelengine.DataModel.JProductDataItem;


import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    public static long sgetCurrentDatetime(){
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
                ResultSet rs = stmt.executeQuery("SELECT * FROM tbproduct WHERE productname='"+dsname+"' LIMIT 1") ;
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
                ResultSet rs = stmt.executeQuery("SELECT * FROM tbproduct WHERE pid="+mysqlPid+" LIMIT 1") ;
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

    public int rdbNewOffTask1( String script, String uid,String path,String dt) {
        try
        {
            String query = " insert into tbOfflineTask (scriptContent, outProductId, outDatetime,"
                    +" startTime, uid, stype,"
                    + "path, htable, hpid, "
                    +" hcol, hfami, hpidlen, hxylen, status )"
                    + " values (?,?,?, ?,?,?, ?,?,?, ?, ?,?,?,? )";
            // create the mysql insert preparedstatement
            PreparedStatement preparedStmt = JRDBHelperForWebservice.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            preparedStmt.setString (1, script);
            preparedStmt.setInt (2, 0);
            preparedStmt.setLong    (3, Long.parseLong(dt));
            long starttime = this.getCurrentDatetime();
            preparedStmt.setLong(4,starttime);
            preparedStmt.setInt(5, Integer.parseInt(uid)) ;
            preparedStmt.setInt(6,1);//script-type=1

            preparedStmt.setString(7, path);
            preparedStmt.setString( 8, WConfig.sharedConfig.hbaseuserfiletable) ;//hbase table name
            preparedStmt.setInt( 9, 1);//hbase pid
            preparedStmt.setLong( 10 , starttime);// hbase column name.

            preparedStmt.setString(11 , "tiles");
            preparedStmt.setInt(12,1);
            preparedStmt.setInt(13,2);
            preparedStmt.setInt(14,0);

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
                jscript.scriptContent = "...";
                jscript.updateTime = rs.getLong("updateTime");
                jscript.type = rs.getInt("type");

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
            ResultSet rs = stmt.executeQuery("SELECT sid,title,scriptcontent,updatetime,uid,type "
                    +" FROM tbscript WHERE sid="+sid+" LIMIT 1") ;
            if (rs.next()) {
                JScript jscript = new JScript();
                jscript.sid = rs.getInt("sid");
                jscript.uid = rs.getInt("uid");
                jscript.title = rs.getString("title");
                jscript.scriptContent = rs.getString("scriptContent");
                jscript.updateTime = rs.getLong("updateTime");
                jscript.type = rs.getInt("type");
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
            ResultSet rs = stmt.executeQuery("SELECT uid,uname "
                    +" FROM tbUser WHERE uname='"+uname+"' LIMIT 1") ;
            if (rs.next()) {
                JUser user = new JUser();
                user.uid = rs.getInt("uid");
                user.uname = rs.getString("uname");
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
                    +" FROM tbStyle WHERE styleid="+sid+" LIMIT 1") ;
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
                    +" FROM tbStyle WHERE styleid="+sid+" LIMIT 1") ;
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

    //2021-3-23 获取一个简短的产品信息
    public JProduct rdbGetProductForAPI(int pid) throws SQLException {
        JProduct result = new JProduct();
        Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM tbproduct WHERE pid="+pid+" limit 1");
        if (rs.next()) {
            int uid = rs.getInt("userid");
            String name = rs.getString("name") ;
            String info = rs.getString("info");
            JProduct pdt = new Gson().fromJson(info, JProduct.class) ;
            pdt.pid = pid ;
            pdt.userid = uid ;
            pdt.name = name ;
            result = pdt ;
        }
        return result ;
    }
    //2021-3-23
    public ArrayList<JProduct> rdbGetProducts() throws SQLException {
        ArrayList<JProduct> result = new ArrayList<>() ;
        Statement stmt = JRDBHelperForWebservice.getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM tbproduct WHERE userid=0 ") ;
        while (rs.next()) {
            int pid = rs.getInt("pid");
            int uid = rs.getInt("userid");
            String name = rs.getString("name") ;
            String info = rs.getString("info");
            JProduct pdt = new Gson().fromJson(info, JProduct.class) ;
            pdt.pid = pid ;
            pdt.userid = uid ;
            pdt.name = name ;

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
                Statement stmtpd = JRDBHelperForWebservice.getConnection().createStatement();
                ResultSet rspd= stmtpd.executeQuery("SELECT * FROM tbproductdisplay WHERE pid='"+
                        pdt.pid + "' LIMIT 1 ") ;
                if( rspd.next() ){
                    pdt.productDisplay.dpid = rspd.getInt("dpid") ;
                    pdt.productDisplay.pid = rspd.getInt("pid") ;

                    pdt.productDisplay.satellite = rspd.getString("satellite") ;
                    pdt.productDisplay.sensor = rspd.getString("sensor") ;
                    pdt.productDisplay.productname = rspd.getString("productname") ;
                    pdt.productDisplay.productdescription = rspd.getString("productdescription") ;
                    pdt.productDisplay.thumb = rspd.getString("thumb") ;

                    pdt.productDisplay.visible = rspd.getInt("visible") ;
                }
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
            result.add(aa) ;
        }
        return result ;
    }
}
