package com.pixelengine.DAO;

import com.pixelengine.DTO.ZonalStatDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface ZonalStatDAO extends JpaRepository<ZonalStatDTO,Long>  {
    //不考虑删除项（4）
    @Query( nativeQuery = true, value = "SELECT  * FROM tbofftaskzonalstat where uid=:userid AND status<>4 ORDER by createtime")
    List<ZonalStatDTO> findAllByUserid(@Param("userid") Long userid);
}
