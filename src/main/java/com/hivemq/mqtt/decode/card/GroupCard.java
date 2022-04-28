package com.hivemq.mqtt.decode.card;

import com.google.gson.annotations.Expose;

import java.time.LocalDateTime;

/**
 * 群信息Model
 *
 * @author qiujuer Email:qiujuer.live.cn
 */
public class GroupCard {
    @Expose
    private String id;// Id
    @Expose
    private String number;
    @Expose
    private String name;// 名称
    @Expose
    private String desc;// 描述
    @Expose
    private String picture;// 群图片
    @Expose
    private String ownerId;// 创建者Id
    @Expose
    private int notifyLevel;// 对于当前用户的通知级别
    @Expose
    private LocalDateTime joinAt;// 加入时间
    @Expose
    private LocalDateTime modifyAt;// 最后修改时间

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public int getNotifyLevel() {
        return notifyLevel;
    }

    public void setNotifyLevel(int notifyLevel) {
        this.notifyLevel = notifyLevel;
    }

    public LocalDateTime getJoinAt() {
        return joinAt;
    }

    public void setJoinAt(LocalDateTime joinAt) {
        this.joinAt = joinAt;
    }

    public LocalDateTime getModifyAt() {
        return modifyAt;
    }

    public void setModifyAt(LocalDateTime modifyAt) {
        this.modifyAt = modifyAt;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}
