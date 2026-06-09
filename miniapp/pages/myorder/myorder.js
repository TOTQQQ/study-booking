const request = require('../../utils/request.js');

Page({
  data: {
    validOrders: [],
    loading: false,
    refreshing: false
  },

  onShow() {
    this.loadOrders();
  },

  onPullDownRefresh() {
    this.loadOrders(true);
  },

  loadOrders(isPullDown = false) {
    const token = wx.getStorageSync('token');
    if (!token) {
      this.setData({ validOrders: [] });
      if (isPullDown) wx.stopPullDownRefresh();
      return;
    }
    
    this.setData({ loading: true });
    
    // 获取我的预约列表（全部状态）
    request({
      url: '/api/reservation/my',
      method: 'GET',
      data: { page: 1, size: 50 }
    }).then(data => {
      const allOrders = (data.records || []).map(this.formatOrder);
      // 筛选待签到(1)和已签到(2)的有效预约
      const validOrders = allOrders.filter(item => item.status === 1 || item.status === 2);
      
      this.setData({ validOrders, loading: false });
      
      if (isPullDown) {
        wx.stopPullDownRefresh();
        wx.showToast({ title: '已刷新', icon: 'success' });
      }
    }).catch(err => {
      console.error('加载预约失败:', err);
      this.setData({ validOrders: [], loading: false });
      if (isPullDown) wx.stopPullDownRefresh();
      wx.showToast({ title: err.message || '加载失败', icon: 'none' });
    });
  },

  formatOrder(order) {
    const statusMap = {
      1: { text: '待签到', color: '#ff9800' },
      2: { text: '已签到', color: '#4caf50' },
      3: { text: '已取消', color: '#999' },
      4: { text: '自动取消', color: '#f44336' }
    };
    const status = statusMap[order.status] || { text: '未知', color: '#999' };
    
    return {
      id: order.id,
      reserveNo: order.reserveNo,
      seatCode: order.seatCode,
      roomName: order.roomName,
      date: order.date,
      startTime: order.startTime,
      endTime: order.endTime,
      status: order.status,
      statusText: status.text,
      statusColor: status.color
    };
  },

  cancelOrder(e) {
    const order = e.currentTarget.dataset.order;
    
    if (order.status !== 1) {
      wx.showToast({ title: '该状态无法取消', icon: 'none' });
      return;
    }
    
    wx.showModal({
      title: '确认取消',
      content: '确定要取消该预约吗？取消后将释放座位。',
      confirmColor: '#f44336',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '取消中' });
          
          request({
            url: '/api/reservation/cancel',
            method: 'POST',
            data: { reservationId: order.id }
          }).then(() => {
            wx.hideLoading();
            wx.showToast({ title: '已取消', icon: 'success' });
            this.loadOrders();
          }).catch(err => {
            wx.hideLoading();
            wx.showToast({ title: err.message || '取消失败', icon: 'none' });
          });
        }
      }
    });
  },

  viewDetail(e) {
    const order = e.currentTarget.dataset.order;
    wx.navigateTo({
      url: `/pages/reservation-detail/reservation-detail?id=${order.id}`
    });
  },

  goCheckin(e) {
    const order = e.currentTarget.dataset.order;
    if (order.status !== 1) {
      wx.showToast({ title: '该预约不需要签到', icon: 'none' });
      return;
    }
    wx.navigateTo({
      url: `/pages/checkin/checkin?id=${order.id}`
    });
  }
});