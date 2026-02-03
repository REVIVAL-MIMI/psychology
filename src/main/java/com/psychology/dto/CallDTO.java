package com.psychology.dto;

import lombok.Data;

public class CallDTO {

    @Data
    public static class Signal {
        private String type; // offer | answer | ice | hangup
        private Long senderId;
        private Long receiverId;
        private String sdp;
        private String candidate;
        private String sdpMid;
        private Integer sdpMLineIndex;
        private String reason;
    }
}
