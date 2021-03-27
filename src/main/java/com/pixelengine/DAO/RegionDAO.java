package com.pixelengine.DAO;


import com.pixelengine.DTO.RegionDTO;
import com.pixelengine.DTO.StyleDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface RegionDAO extends JpaRepository<RegionDTO,Long> {

    @Query( nativeQuery = true, value = "SELECT  * FROM tbregion where uid=:uid ")
    List<RegionDTO> findAllByUserid(@Param("uid") Long uid);

    @Query( nativeQuery = true, value = "SELECT  * FROM tbregion where name like %:key% AND uid=:userid ")
    List<RegionDTO> findByName(@Param("userid") Long userid,@Param("key") String key);

}