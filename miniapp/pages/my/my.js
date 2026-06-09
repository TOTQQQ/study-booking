const request = require('../../utils/request.js');

Page({
  data: {
    userInfo: null,
    todayReservation: null,
    statsCount: {
      totalReservations: 0,
      violationCount: 0
    }
  },

  onShow() {
    this.loadUserInfo();
    this.loadTodayReservation();
    this.loadStats();
  },

  loadUserInfo() {
    const userInfo = wx.getStorageSync('userInfo');
    this.setData({ userInfo });
  },

  // 加载今日预约
  loadTodayReservation() {
    const token = wx.getStorageSync('token');
    if (!token) return;
    
    request({
      url: '/api/reservation/current',
      method: 'GET'
    }).then(order => {
      this.setData({ todayReservation: order });
    }).catch(() => {
      this.setData({ todayReservation: null });
    });
  },

  // 加载统计数据
  loadStats() {
    const token = wx.getStorageSync('token');
    if (!token) return;
    
    request({
      url: '/api/reservation/my',
      method: 'GET',
      data: { page: 1, size: 100 }
    }).then(data => {
      this.setData({
        statsCount: {
          totalReservations: data.total || 0,
          violationCount: 0  // 违约次数需要后端提供单独接口
        }
      });
    }).catch(err => {
      console.error('加载统计失败:', err);
    });
  },

  // 跳转我的预约
  goMyOrder() {
    wx.navigateTo({ url: '/pages/myorder/myorder' });
  },
  
  // 跳转历史记录
  goHistory() {
    wx.navigateTo({ url: '/pages/history/history' });
  },

  // 跳转签到页面
  goCheckin() {
    const token = wx.getStorageSync('token');
    if (!token) {
      wx.navigateTo({ url: '/pages/login/login' });
      return;
    }
    
    request({
      url: '/api/reservation/current',
      method: 'GET'
    }).then(order => {
      if (order && order.status === 1) {
        wx.navigateTo({ url: `/pages/checkin/checkin?id=${order.id}` });
      } else if (order && order.status === 2) {
        wx.showToast({ title: '今日已完成签到', icon: 'none' });
      } else {
        wx.showModal({
          title: '提示',
          content: '当前没有待签到的预约，请先预约座位',
          showCancel: false,
          success: () => {
            wx.switchTab({ url: '/pages/seat/seat' });
          }
        });
      }
    }).catch(() => {
      wx.showModal({
        title: '提示',
        content: '当前没有待签到的预约，请先预约座位',
        showCancel: false,
        success: () => {
          wx.switchTab({ url: '/pages/seat/seat' });
        }
      });
    });
  },

  // 退出登录
  logout() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          // 调用退出接口
          const token = wx.getStorageSync('token');
          if (token) {
            request({
              url: '/api/auth/logout',
              method: 'POST'
            }).catch(() => {});
          }
          
          // 清除本地存储
          wx.removeStorageSync('token');
          wx.removeStorageSync('refreshToken');
          wx.removeStorageSync('userInfo');
          
          wx.showToast({ title: '已退出', icon: 'success' });
          setTimeout(() => {
            wx.reLaunch({ url: '/pages/login/login' });
          }, 1000);
        }
      }
    });
  }
});