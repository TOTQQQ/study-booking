const request = require('../../utils/request.js');

Page({
  data: {
    reservationId: null,
    orderInfo: null,
    countdown: 0,
    canCheckin: false,
    checking: false
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ reservationId: parseInt(options.id) });
      this.loadOrderDetail();
      this.checkCanCheckin();
    } else {
      wx.showToast({ title: '参数错误', icon: 'none' });
      setTimeout(() => wx.navigateBack(), 1500);
    }
  },

  onUnload() {
    if (this.timer) {
      clearInterval(this.timer);
    }
  },

  loadOrderDetail() {
    const { reservationId } = this.data;
    if (!reservationId) return;
    
    wx.showLoading({ title: '加载中' });
    
    request({
      url: `/api/reservation/${reservationId}`,
      method: 'GET'
    }).then(data => {
      wx.hideLoading();
      
      const statusMap = {
        1: { text: '待签到', color: '#ff9800' },
        2: { text: '已签到', color: '#4caf50' },
        3: { text: '已取消', color: '#999' },
        4: { text: '自动取消', color: '#f44336' }
      };
      const status = statusMap[data.status] || { text: '未知', color: '#999' };
      
      this.setData({
        orderInfo: {
          id: data.id,
          reserveNo: data.reserveNo,
          seatCode: data.seatCode,
          roomName: data.roomName,
          date: data.date,
          startTime: data.startTime,
          endTime: data.endTime,
          status: data.status,
          statusText: status.text,
          statusColor: status.color
        }
      });
    }).catch(err => {
      wx.hideLoading();
      wx.showToast({ title: err.message || '加载失败', icon: 'none' });
      setTimeout(() => wx.navigateBack(), 1500);
    });
  },

  checkCanCheckin() {
    const { reservationId } = this.data;
    if (!reservationId) return;
    
    request({
      url: '/api/checkin/check',
      method: 'GET',
      data: { reservationId }
    }).then(canCheckin => {
      this.setData({ canCheckin: canCheckin });
      if (canCheckin) {
        this.startCountdown();
      }
    }).catch(err => {
      console.error('检查签到状态失败:', err);
    });
  },

  startCountdown() {
    const { reservationId } = this.data;
    if (!reservationId) return;
    
    request({
      url: '/api/checkin/countdown',
      method: 'GET',
      data: { reservationId }
    }).then(countdown => {
      if (countdown > 0) {
        this.setData({ countdown: countdown });
        
        this.timer = setInterval(() => {
          if (this.data.countdown <= 1) {
            clearInterval(this.timer);
            this.setData({ countdown: 0, canCheckin: false });
            wx.showToast({ title: '签到时间已过', icon: 'none' });
          } else {
            this.setData({ countdown: this.data.countdown - 1 });
          }
        }, 1000);
      } else {
        this.setData({ canCheckin: false });
      }
    }).catch(err => {
      console.error('获取倒计时失败:', err);
    });
  },

  formatCountdown() {
    const seconds = this.data.countdown;
    if (seconds <= 0) return '已超时';
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${minutes}分${secs}秒`;
  },

  manualCheckin() {
    const { reservationId, canCheckin, checking } = this.data;
    
    if (!canCheckin) {
      wx.showToast({ title: '当前无法签到', icon: 'none' });
      return;
    }
    
    if (checking) return;
    
    this.setData({ checking: true });
    
    request({
      url: '/api/checkin/confirm',
      method: 'POST',
      data: { reservationId }
    }).then(data => {
      this.setData({ checking: false });
      wx.showToast({ title: '签到成功', icon: 'success' });
      
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    }).catch(err => {
      this.setData({ checking: false });
      wx.showToast({ title: err.message || '签到失败', icon: 'none' });
    });
  },

  scanCode() {
    wx.scanCode({
      success: (res) => {
        const seatId = parseInt(res.result);
        if (isNaN(seatId)) {
          wx.showToast({ title: '无效二维码', icon: 'none' });
          return;
        }
        
        wx.showLoading({ title: '签到中' });
        
        request({
          url: '/api/checkin/scan',
          method: 'POST',
          data: { seatId }
        }).then(data => {
          wx.hideLoading();
          wx.showToast({ title: '签到成功', icon: 'success' });
          setTimeout(() => {
            wx.navigateBack();
          }, 1500);
        }).catch(err => {
          wx.hideLoading();
          wx.showToast({ title: err.message || '签到失败', icon: 'none' });
        });
      },
      fail: () => {
        wx.showToast({ title: '扫码失败', icon: 'none' });
      }
    });
  }
});