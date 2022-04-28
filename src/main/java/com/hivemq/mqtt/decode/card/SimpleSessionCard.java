/**
 * @Classname SimpleSessionCard
 * @Description TODO
 * @Date 2022/4/5 10:04
 * @Created by Lenovo
 */
package com.hivemq.mqtt.decode.card;

import com.google.gson.annotations.Expose;

import java.time.LocalDateTime;

/**
 *
 * 查询服务端会话状态返回给客户端
 * 拉取最新消息时，最新消息时间在latestMsgTime之后，
 * 拉取未读离线消息时，消息时间在lastReadMsgTime- latestMsgTime之间
 *
 *
 * 情景一：
 * t3-------已读-----t2---未读-----t1------已读-----t0
 * |                |             |
 * 当前会话         当前页面       上次已读
 * 最新消息         最后一条       消息时间
 * 时间            消息时间
 *
 * if(t2 <= t1) { 加载本地历史消息， t1 = t3 }
 * else 从服务器加载离线未读消息
 *
 * 存在问题：
 * t2-t1的未读消息推送到本地了，但还是去从服务器去拉取
 *
 * 当请求返回时，if(！hasmore) t1 = t3，不再从服务器拉取
 *
 *
 *
 */

public class SimpleSessionCard {

    public static final int TYPE_GROUP = 0;
    public static final int TYPE_USER = 1;

    //会话id 同时也是群id或者用户id
    @Expose
    private String id;

    //会话类型
    @Expose
    private int type;

    //接受者，会话的另一方
    @Expose
    private String receiveId;

    //未读消息数
    @Expose
    private int unreadCount;

    //上一次已读消息时间, 用于加载更多离线消息
    //（当请求返回时该值为最后一条返回消息的时间，备用）
    @Expose
    private LocalDateTime lastReadMsgTime;

    //拉取最新离线消息时为本地最新的消息时间，拉取离线未读消息时为会话当前页最后一条消息时间，
    //（请求返回时该值为首条返回消息的时间，备用）
    @Expose
    private LocalDateTime latestMsgTime;

    //是否有更多的消息
    @Expose
    private boolean hasMore;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getLastReadMsgTime() {
        return lastReadMsgTime;
    }

    public void setLastReadMsgTime(LocalDateTime lastReadMsgTime) {
        this.lastReadMsgTime = lastReadMsgTime;
    }

    public LocalDateTime getLatestMsgTime() {
        return latestMsgTime;
    }

    public void setLatestMsgTime(LocalDateTime latestMsgTime) {
        this.latestMsgTime = latestMsgTime;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getReceiveId() {
        return receiveId;
    }

    public void setReceiveId(String receiveId) {
        this.receiveId = receiveId;
    }
}
