package com.pixelengine.DTO;

import javax.persistence.*;
import java.util.Date;


@Entity
@Table(name="tbstyle")
public class StyleDTO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "styleid")
    private Long styleid;

    @Column(name = "stylecontent")
    private String styleContent;

    @Column(name = "userid")
    private Long userid;

    @Column(name = "description")
    private String description;

    @Column(name = "createtime")
    private Date createtime;

    @Column(name = "updatetime")
    private Date updatetime;

    public Long getStyleid() {
        return styleid;
    }

    public void setStyleid(Long styleid) {
        this.styleid = styleid;
    }

    public String getStyleContent() {
        return styleContent;
    }

    public void setStyleContent(String styleContent) {
        this.styleContent = styleContent;
    }

    public Long getUserid() {
        return userid;
    }

    public void setUserid(Long userid) {
        this.userid = userid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreatetime() {
        return createtime;
    }

    public void setCreatetime(Date createtime) {
        this.createtime = createtime;
    }

    public Date getUpdatetime() {
        return updatetime;
    }

    public void setUpdatetime(Date updatetime) {
        this.updatetime = updatetime;
    }
}
