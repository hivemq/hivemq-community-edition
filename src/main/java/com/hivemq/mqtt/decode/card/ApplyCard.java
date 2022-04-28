package com.hivemq.mqtt.decode.card;

import com.google.gson.annotations.Expose;

import java.time.LocalDateTime;

/**
 * 申请请求的Card, 用于推送一个申请请求
 *
 * @author qiujuer Email:qiujuer.live.cn
 */
public class ApplyCard {
    //申请id
    @Expose
    private String id;
    // 申请人的Id
    @Expose
    private String applicantId;
    // 附件
    @Expose
    private String attach;
    // 描述
    @Expose
    private String desc;
    // 目标的类型
    @Expose
    private int type;
    // 目标（群／人...的ID）
    @Expose
    private String targetId;
    // 创建时间
    @Expose
    private LocalDateTime createAt;
    // 更新时间
    @Expose
    private LocalDateTime updateAt;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAttach() {
        return attach;
    }

    public void setAttach(String attach) {
        this.attach = attach;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(String applicantId) {
        this.applicantId = applicantId;
    }

    public LocalDateTime getCreateAt() {
        return createAt;
    }

    public void setCreateAt(LocalDateTime createAt) {
        this.createAt = createAt;
    }

    public LocalDateTime getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(LocalDateTime updateAt) {
        this.updateAt = updateAt;
    }
}
