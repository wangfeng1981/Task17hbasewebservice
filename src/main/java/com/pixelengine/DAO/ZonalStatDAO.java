package com.pixelengine.DAO;

//import com.pixelengine.DTO.ZonalStatDTO;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.*;
//import org.springframework.data.repository.query.Param;

import java.util.List;

//deprecated 2022-9-14

//public interface ZonalStatDAO extends JpaRepository<ZonalStatDTO,Long>  {
//    //不考虑删除项（4）
//    @Query( nativeQuery = true, value = "SELECT  * FROM tbofftaskzonalstat where uid=:userid AND mode=:mode  AND status<>4 ORDER by createtime DESC")
//    List<ZonalStatDTO> findAllByUserid(@Param("userid") Long userid, @Param("mode") int mode);
//
//    //检索全部序列任务（实况+历史）
//    @Query( nativeQuery = true, value = "SELECT  * FROM tbofftaskzonalstat where uid=:userid AND mode in (1,2)  AND status<>4 ORDER by createtime DESC")
//    List<ZonalStatDTO> findAllXLTaskByUserid(@Param("userid") Long userid);
//}
