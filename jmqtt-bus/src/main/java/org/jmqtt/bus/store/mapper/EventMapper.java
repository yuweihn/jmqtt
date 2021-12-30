package org.jmqtt.bus.store.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jmqtt.bus.store.daoobject.EventDO;

import java.util.List;

public interface EventMapper {

    @Insert("insert into jmqtt_event (content,gmt_create,node_ip,event_code) values "
            + "(#{content},#{gmtCreate},#{nodeIp},#{eventCode})")
    Long sendEvent(EventDO eventDO);


    @Select("select id,content,gmt_create,node_ip,event_code from jmqtt_event "
            + "where id > #{offset} order by id asc limit #{maxNum}")
    List<EventDO> consumeEvent(@Param("offset") long offset, @Param("maxNum") int maxNum);

    @Select("SELECT max(id) FROM jmqtt_event")
    Long getMaxOffset();
}
