package com.studyroom.vo;

import java.util.List;

public class SeatVO {
    private Integer id;
    private String seatCode;
    private Integer x;
    private Integer y;
    private Integer status;
    private List<TimeSlotStatus> timeSlots;
    
    // getter and setter
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getSeatCode() {
        return seatCode;
    }
    
    public void setSeatCode(String seatCode) {
        this.seatCode = seatCode;
    }
    
    public Integer getX() {
        return x;
    }
    
    public void setX(Integer x) {
        this.x = x;
    }
    
    public Integer getY() {
        return y;
    }
    
    public void setY(Integer y) {
        this.y = y;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public List<TimeSlotStatus> getTimeSlots() {
        return timeSlots;
    }
    
    public void setTimeSlots(List<TimeSlotStatus> timeSlots) {
        this.timeSlots = timeSlots;
    }
    
    // 内部类 TimeSlotStatus
    public static class TimeSlotStatus {
        private Integer timePeriodId;
        private String startTime;
        private String endTime;
        private Integer status;
        
        public Integer getTimePeriodId() {
            return timePeriodId;
        }
        
        public void setTimePeriodId(Integer timePeriodId) {
            this.timePeriodId = timePeriodId;
        }
        
        public String getStartTime() {
            return startTime;
        }
        
        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }
        
        public String getEndTime() {
            return endTime;
        }
        
        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }
        
        public Integer getStatus() {
            return status;
        }
        
        public void setStatus(Integer status) {
            this.status = status;
        }
    }
}