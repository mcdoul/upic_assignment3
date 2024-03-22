package com.upic.server.model;
/**
 * response data
 * For responses with status codes other than 201, return messages in json format.
 */
public class ResponseMessage {

    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ResponseMessage() {
    }

    public ResponseMessage(String message) {
        this.message = message;
    }
}
