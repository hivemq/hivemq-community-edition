/**
 * @Classname FileRecordCard
 * @Description TODO
 * @Date 2022/4/23 17:53
 * @Created by Lenovo
 */
package com.hivemq.mqtt.decode.card;

import com.google.gson.annotations.Expose;


import java.time.LocalDateTime;

public class FileRecordCard {
    //文件记录ID
    @Expose
    private String id;
    //文件路径
    @Expose
    private String path;
    //文件名
    @Expose
    private String filename;
    //文件大小
    @Expose
    private String fileSize;
    // 文件来源：群文件，用户文件
    @Expose
    private int type;
    //上传目的地，群，或者用户
    @Expose
    private String targetId;
    //上传者Id
    @Expose
    private String uploaderId;
    //上传时间
    @Expose
    private LocalDateTime uploadTime;
    //状态
    @Expose
    private int status;

    public FileRecordCard() {

    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }


    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(String uploaderId) {
        this.uploaderId = uploaderId;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
