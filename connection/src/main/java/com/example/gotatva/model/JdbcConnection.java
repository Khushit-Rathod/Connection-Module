package com.example.gotatva.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JdbcConnection {
    private String connectionName;
    private String email;
    private String bpIncoming;
    private String bpOutgoing;
    private String bpAcknowledgement;
    private String bpSyncResponse;
    private String url;
    private String username;
    private String password;
    private String customQuery;
    private boolean active;
    private int autoTestInterval;
}