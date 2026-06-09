const request = require('../../utils/request.js');

Page({
  data: {
    stats: {
      totalSeats: 200,
      todayReservations: 0,
      todayCheckedIn: 0
    },
    todayDate: '',
    loading: false
  },

  onShow() {
    this.initDate();
    this.loadStats();
  },

  initDate() {
    const today = new Date();
    const year = today.getFullYear();
    const month = (today.getMonth() + 1).toString().padStart(2, '0');
    const day = today.getDate().toString().padStart(2, '0');
    this.setData({ todayDate: `${year}-${month}-${day}` });
  },

  loadStats() {
    this.setData({ loading: true });
    
    // 获取我的预约列表来计算今日统计
    request({
      url: '/api/reservation/my',
      method: 'GET',
      data: { page: 1, size: 100 }
    }).then(data => {
      const todayStr = this.data.todayDate;
      const todayOrders = (data.records || []).filter(order => order.date === todayStr);
      const todayCheckedIn = todayOrders.filter(order => order.status === 2).length;
      
      this.setData({
        stats: {
          totalSeats: 200,
          todayReservations: todayOrders.length,
          todayCheckedIn: todayCheckedIn
        },
        loading: false
      });
    }).catch(err => {
      console.error('加载统计失败:', err);
      this.setData({ 
        loading: false,
        stats: {
          totalSeats: 200,
          todayReservations: 0,
          todayCheckedIn: 0
        }
      });
    });
  },

  // 跳转选座位
  goSeat() {
    const token = wx.getStorageSync('token');
    if (!token) {
      wx.navigateTo({ url: '/pages/login/login' });
      return;
    }
    wx.switchTab({ url: '/pages/seat/seat' });
  },

  // 跳转签到
  goCheckin() {
    const token = wx.getStorageSync('token');
    if (!token) {
      wx.navigateTo({ url: '/pages/login/login' });
      return;
    }
    
    // 获取当前有效预约
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

  // 跳转我的预约
  goMy() {
    const token = wx.getStorageSync('token');
    if (!token) {
      wx.navigateTo({ url: '/pages/login/login' });
      return;
    }
    wx.switchTab({ url: '/pages/my/my' });
  }
});