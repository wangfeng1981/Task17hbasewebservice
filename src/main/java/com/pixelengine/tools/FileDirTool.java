package com.pixelengine.tools;

import com.pixelengine.WConfig;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileDirTool {
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
}
