
package com.hivemq.mqtt.decode;

import com.google.gson.reflect.TypeToken;
import com.hivemq.mqtt.decode.card.*;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.util.LocalDateTimeUtils;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

public class Factory {

    //更新publish负载内容
    public static void refresh(final PUBLISH publish) {
        updateTime(publish);
    }

    //更新消息时间
    public static void updateTime(final PUBLISH publish) {
        String jsonStr = new String(publish.getPayload(),StandardCharsets.UTF_8);
        jsonStr = updateTime(jsonStr,publish.getTimestamp());
        publish.setPayload(jsonStr.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 处理推送来的消息
     *
     * @param str 消息
     */
    public static String updateTime(String str,long timestamp) {
        PushModel model = PushModel.decode(str);
        if (model == null)
            return str;

        // 对推送集合进行遍历
        for (PushModel.Entity entity : model.getEntities()) {

            switch (entity.type) {
                case PushModel.ENTITY_TYPE_MESSAGE: {
                    // 普通消息
                    MessageCard card = GsonProvider.getGson().fromJson(entity.content, MessageCard.class);
                    card.setCreateAt(LocalDateTimeUtils.timestampToDatetime(timestamp));
                    entity.createAt = card.getCreateAt();
                    entity.content = GsonProvider.getGson().toJson(card,MessageCard.class);
                    break;
                }
                case PushModel.ENTITY_TYPE_LOGOUT:
                case PushModel.ENTITY_TYPE_APPLY:
                case PushModel.ENTITY_TYPE_REPLY:
                case PushModel.ENTITY_TYPE_ADD_FRIEND:
                case PushModel.ENTITY_TYPE_ADD_GROUP:
                case PushModel.ENTITY_TYPE_ADD_GROUP_MEMBERS:
                case PushModel.ENTITY_TYPE_MODIFY_GROUP_MEMBERS:
                case PushModel.ENTITY_TYPE_EXIT_GROUP_MEMBERS: {
                    //暂时走http，此处不用修改
                    break;
                }

            }
        }

        return model.getPushString();
    }
}
