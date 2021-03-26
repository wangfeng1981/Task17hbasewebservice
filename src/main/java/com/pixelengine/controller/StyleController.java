package com.pixelengine.controller;


import com.pixelengine.DAO.StyleDAO;
import com.pixelengine.DTO.StyleDTO;
import com.pixelengine.DataModel.RestResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value="/style")
@CrossOrigin
public class StyleController {
    @Autowired
    StyleDAO styleDao ;


    @PostMapping(value="/new")
    public RestResult styleNew(String stylecontent, String userid
        , String description )
    {
        StyleDTO s1 = new StyleDTO() ;
        s1.setStyleContent(stylecontent);
        s1.setDescription(description);
        s1.setUserid( Long.valueOf(userid));
        s1.setCreatetime(Calendar.getInstance().getTime());
        s1.setUpdatetime(Calendar.getInstance().getTime());
        StyleDTO newStyle = styleDao.save(s1) ;
        RestResult result = new RestResult() ;
        result.setState(0);
        result.setData(newStyle);
        return result;
    }

    @PostMapping(value="/edit")
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
    public RestResult styleGet(@PathVariable("styleid") String styleid)
    {
        Optional<StyleDTO> newStyle = styleDao.findById( Long.parseLong(styleid)) ;
        RestResult result = new RestResult() ;
        result.setState(0);
        result.setData(newStyle);
        return result;
    }

    @GetMapping(value="/remove/{styleid}")
    public RestResult styleRemove(@PathVariable("styleid") String styleid)
    {
        styleDao.deleteById( Long.parseLong(styleid));
        RestResult result = new RestResult() ;
        result.setState(0);
        return result;
    }

    @GetMapping(value="/list/{userid}")
    public RestResult styleList(@PathVariable("userid") String userid)
    {
        List<StyleDTO> allstyle = styleDao.findAllByUserid( Long.parseLong(userid)) ;
        RestResult result = new RestResult() ;
        result.setState(0);
        result.setData(allstyle);
        return result;
    }

}
