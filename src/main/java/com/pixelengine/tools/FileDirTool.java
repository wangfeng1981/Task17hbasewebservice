package com.pixelengine.tools;
/// 2022-3-5 2210
/// 2022-4-4 add writeBinaryFile method.
/// udpate 2022-4-9
/// update 2022-4-18 createYmdHmsFilename 只创建文件路径，如果需要建立子目录，但是不具体创建文件

import com.pixelengine.DataModel.WConfig;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Random;

public class FileDirTool {
    public static class FileNamerResult {
        public int state =99 ;//0-ok, others bad.
        public String message = "";
        public FileNamer data = null ;
    }

    public static class FileNamer {
        public String relfilename ;//filename under absRootdir
        public String absfilename ;//absfilename = absRootdir + relfilename.
    }

    //通过日期和时间建立一个文件路径，只建立路径不写文件 文件名格式为 absRootdir+{yyyyMMdd}/{prefix}{HHmmss}-{rrrr}{tail}

    /**
     * @param rootdir /var/www/html/pe/
     * @param sub1name omc_out
     * @param prefix empty or some-
     * @param tail empty or .xxx
     * @return  /var/www/html/pe/{sub1name}/{yyyyMMdd}/{prefix}{HHmmss}-{rrrr}{tail}
     */
    static  public  FileNamerResult buildDatetimeSubdirAndFilename(
            String rootdir,String sub1name,String prefix,String tail){

        FileNamerResult rr = new FileNamerResult() ;
        if( rootdir.length()==0 ){
            rr.state = 0 ;
            rr.message = "empty rootdir." ;
            return rr ;
        }

        if( rootdir.charAt(rootdir.length()-1) != '/' ){
            rootdir+="/" ;
        }

        Date date = new Date();
        SimpleDateFormat datetimeFormat1 = new SimpleDateFormat("yyyyMMdd");
        String yyyyMMddStr = datetimeFormat1.format(date);
        SimpleDateFormat datetimeFormat2 = new SimpleDateFormat("HHmmss");
        String hhmmssStr = datetimeFormat2.format(date);
        String randStr = String.format("%04d",new Random().nextInt(9999)) ;
        String newFileName = hhmmssStr+"-"+randStr ;
        //check rootdir ok
        if( checkDirExistsOrCreate(rootdir) == false){
            rr.state=2 ;
            rr.message = "rootdir not exist and failed to make." ;
            return rr ;
        }

        String absSub1name = rootdir + sub1name + "/" ;
        boolean sub1ok = checkDirExistsOrCreate(absSub1name) ;
        if( sub1ok==false ){
            rr.state = 3 ;
            rr.message = "failed to make sub1dir:" + absSub1name ;
            return rr ;
        }

        //check sub2dir ok
        String sub2dir =  absSub1name +  yyyyMMddStr  + "/" ;
        boolean sub2ok = checkDirExistsOrCreate(sub2dir) ;
        if( sub2ok==false ){
            rr.state = 4 ;
            rr.message = "failed to make sub2dir:" + sub2dir ;
            return rr;
        }

        FileNamer fn = new FileNamer() ;
        fn.absfilename = sub2dir + prefix + newFileName + tail ;
        fn.relfilename = sub1name + "/" + yyyyMMddStr + "/" + prefix + newFileName + tail ;
        rr.state = 0 ;
        rr.message = "" ;
        rr.data = fn ;
        return rr ;
    }



    ///ext: .js .json .xml
    static public String makeDatetimeFilename(String rootdir, String prefix, String ext){
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String da = sdf.format(date);
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMddhhmmss");
        String da1 = sdf1.format(date);
        String yearstr = da.substring(0,4) ;
        String thepath = rootdir + "/" +yearstr + "/" +da +"/";

        File dirfile = new File( thepath) ;
        File dirfile0 = new File( dirfile.getParent()) ;
        if( dirfile0.exists() == false ){
            dirfile0.mkdirs() ;
        }
        if( dirfile.exists()==false ){
            boolean dirok = dirfile.mkdir() ;
            if( dirok==false ){
                System.out.println("Create dir failed:" + thepath);
                return null ;
            }
        }

        String tempfullname = thepath + prefix + "-" + da1 + ext ;
        int itry = 1;
        while( (new File(tempfullname)).exists()==true ){
            tempfullname = thepath + prefix + "-" + da1 +"-" + itry + ext ;
            ++itry ;
        }
        return tempfullname ;
    }

    static public String dateTimeString()
    {
        Date date = new Date();
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMddhhmmss");
        String dtstr = sdf1.format(date);
        return dtstr ;
    }

    ///根据当前日期构建一个目录，格式 {rootdir}/{subdir}/yyyy/yyyyMMdd/
    ///rootdir必须存在，函数不对rootdir做检查
    ///subdir 可以存在也可以不存在，如果不存在就创建
    ///返回绝对路径和相对路径包含斜杠结尾 0-绝对路径， 1-相对路径
    static public String[] checkAndMakeCurrentYearDateDir(String rootdir, String subdir ){
        Date date = new Date();
        SimpleDateFormat yearformat = new SimpleDateFormat("yyyy");
        String yearstr = yearformat.format(date);
        SimpleDateFormat ymdformat = new SimpleDateFormat("yyyyMMdd");
        String ymdstr = ymdformat.format(date);

        String relativeDir = "/" + subdir + "/" +yearstr + "/" +ymdstr +"/" ;
        String absDir = rootdir + relativeDir ;
        File dirfile = new File( absDir) ;
        if( dirfile.exists() == false ){
            dirfile.mkdirs() ;
        }
        if( dirfile.exists()==false ){
            System.out.println("Create dir failed:" + absDir);
            return null ;
        }
        String[] outdir = new String[2] ;
        outdir[0] = absDir ;
        outdir[1] = relativeDir ;
        return outdir ;
    }

    static public boolean writeToFile(String filename,String content)
    {
        BufferedOutputStream stream = null;
        try {
            stream = new BufferedOutputStream(new FileOutputStream(filename));
            stream.write(content.getBytes());
            stream.flush();
            stream.close();
            return true ;
        }catch (IOException e){
            e.printStackTrace();
            return false ;
        }
    }

    /// 2022-4-4 write string into text file
    static public boolean writeToTextFile(String filename,String content)
    {
        BufferedOutputStream stream = null;
        try {
            stream = new BufferedOutputStream(new FileOutputStream(filename));
            stream.write(content.getBytes());
            stream.flush();
            stream.close();
            return true ;
        }catch (IOException e){
            e.printStackTrace();
            return false ;
        }
    }

    /// 2022-4-4 write bytearray into binary file
    static public boolean writeToBinaryFile(String filename, byte[] bytesContent )
    {
        BufferedOutputStream stream = null;
        try {
            stream = new BufferedOutputStream(new FileOutputStream(filename));
            stream.write(bytesContent);
            stream.flush();
            stream.close();
            return true ;
        }catch (IOException e){
            e.printStackTrace();
            return false ;
        }
    }

    static public  String readFileAsString(String fileName)throws Exception
    {
        String data = "";
        try{
            data = new String(Files.readAllBytes(Paths.get(fileName)));
            return data;
        }catch (Exception ex){
            return null ;
        }
    }

    static public byte[] readFileAsBytes(String fileName)
    {
        try{
            return Files.readAllBytes(Paths.get(fileName));
        }catch (Exception ex){
            System.out.println("readFileAsBytes exception:" + ex.getMessage());
            return null ;
        }
    }

    /**
     * 判断文件夹是否存在,如果不存在创建他
     * @param dirPathStr
     */
    static public boolean checkDirExistsOrCreate(String dirPathStr) {
        File file = new File(dirPathStr) ;
        if (file.exists()) {
            return true ;
        } else {
            return file.mkdir();
        }
    }




}
