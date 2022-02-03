package com.pixelengine.controller;


import com.pixelengine.DAO.StyleDAO;
import com.pixelengine.DTO.StyleDTO;
import com.pixelengine.DataModel.RestResult;
import com.pixelengine.JPeStyle;
import com.pixelengine.JRDBHelperForWebservice;
import com.pixelengine.JUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value="/style")
@CrossOrigin(origins = "*")
public class StyleController {
    @Autowired
    StyleDAO styleDao ;


    @PostMapping(value="/new2")
    @CrossOrigin(origins = "*")
    public RestResult styleNew(@RequestHeader("token") String token,
                               String stylecontent , String description )
    {
        RestResult result = new RestResult() ;
        JUser tempUser = JUser.getUserByToken(token) ;
        if( tempUser == null ){
            result.setData(1);
            result.setMessage("没有用户登录信息");
            return result ;
        }else{
            StyleDTO s1 = new StyleDTO() ;
            s1.setStyleContent(stylecontent);
            s1.setDescription(description);
            s1.setUserid(  (long)tempUser.uid );
            s1.setCreatetime(Calendar.getInstance().getTime());
            s1.setUpdatetime(Calendar.getInstance().getTime());
            StyleDTO newStyle = styleDao.save(s1) ;

            result.setState(0);
            result.setData(newStyle);
            return result;
        }
    }

    @PostMapping(value="/new")
    @CrossOrigin(origins = "*")
    @ResponseBody
    public RestResult styleNewOld( String userid,
                               String stylecontent , String description )
    {
        RestResult result = new RestResult() ;

        {
            StyleDTO s1 = new StyleDTO() ;
            s1.setStyleContent(stylecontent);
            s1.setDescription(description);
            s1.setUserid(  Long.valueOf(userid) );
            s1.setCreatetime(Calendar.getInstance().getTime());
            s1.setUpdatetime(Calendar.getInstance().getTime());
            StyleDTO newStyle = styleDao.save(s1) ;

            result.setState(0);
            result.setData(newStyle);
            return result;
        }
    }

    @PostMapping(value="/edit")
    @CrossOrigin(origins = "*")
    public RestResult styleEdit(String styleid,
                                String stylecontent,
                                String description )
    {
        StyleDTO style1 = styleDao.getOne( Long.parseLong(styleid)) ;
        style1.setDescription(description);
        style1.setStyleContent(stylecontent);
        StyleDTO newStyle = styleDao.save(style1) ;
        RestResult result = new RestResult() ;
        result.setState(0);
        result.setData(newStyle);
        return result;
    }

    @GetMapping(value="/detail/{styleid}")
    @CrossOrigin(origins = "*")
    public RestResult styleGet(@PathVariable("styleid") String styleid)
    {
        JRDBHelperForWebservice rdb = new JRDBHelperForWebservice();
        StyleDTO styleobj = rdb.rdbGetStyle2(Integer.parseInt(styleid));
        RestResult result = new RestResult() ;
        result.setState(0);
        result.setData(styleobj);
        return result;
    }

    @GetMapping(value="/remove/{styleid}")
    @CrossOrigin(origins = "*")
    public RestResult styleRemove(@PathVariable("styleid") String styleid)
    {
        styleDao.deleteById( Long.parseLong(styleid));
        RestResult result = new RestResult() ;
        result.setState(0);
        return result;
    }

    @GetMapping(value="/list/{userid}")
    @CrossOrigin(origins = "*")
    public RestResult styleList(@PathVariable("userid") String userid)
    {
        List<StyleDTO> allstyle = styleDao.findAllByUserid( Long.parseLong(userid)) ;
        RestResult result = new RestResult() ;
        result.setState(0);
        result.setData(allstyle);
        return result;
    }

}
