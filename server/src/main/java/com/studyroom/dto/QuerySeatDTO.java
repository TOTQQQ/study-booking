package com.studyroom.dto;

public class QuerySeatDTO {
    private Integer roomId;
    private String date;
    private Integer timePeriodId;
    
    public Integer getRoomId() {
        return roomId;
    }
    
    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public Integer getTimePeriodId() {
        return timePeriodId;
    }
    
    public void setTimePeriodId(Integer timePeriodId) {
        this.timePeriodId = timePeriodId;
    }
}