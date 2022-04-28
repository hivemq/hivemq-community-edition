/**
 * @Classname ReplyCard
 * @Description 回应
 * @Date 2022/3/28 22:00
 * @Created by Lenovo
 */
package com.hivemq.mqtt.decode.card;

import com.google.gson.annotations.Expose;

import java.time.LocalDateTime;

public class ReplyCard {


    @Expose
    private String id;

    @Expose
    //本回应申请对应的ID
    private String applyId;

    @Expose
    //回应状态，拒绝或同一
    private int result;

    @Expose
    // 描述部分，一般为拒绝理由可以为空
    private String description;

    // 创建时间
    @Expose
    private LocalDateTime createAt;

    // 创建时间
    @Expose
    private LocalDateTime updateAt;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApplyId() {
        return applyId;
    }

    public void setApplyId(String applyId) {
        this.applyId = applyId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreateAt() {
        return createAt;
    }

    public void setCreateAt(LocalDateTime createAt) {
        this.createAt = createAt;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public LocalDateTime getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(LocalDateTime updateAt) {
        this.updateAt = updateAt;
    }
}
