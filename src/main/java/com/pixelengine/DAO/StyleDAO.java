package com.pixelengine.DAO;

import com.pixelengine.DTO.StyleDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StyleDAO extends JpaRepository<StyleDTO,Long> {
    @Query( nativeQuery = true, value = "SELECT  * FROM tbstyle where userid=:userid ")
    List<StyleDTO> findAllByUserid(@Param("userid") Long userid);
}
