package org.jmqtt.broker.store.rdb.daoobject;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString
public class ProductTopicDO extends TenantBase implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long productId;

    private String topic;

    private String authority;

}
