const request = require('../../utils/request.js');

Page({
  data: {
    order: {},
    statusIcon: '',
    checkinDeadline: '',
    loading: false
  },

  onLoad(options) {
    if (options.id) {
      this.loadOrderDetail(options.id);
    } else {
      wx.showToast({ title: '参数错误', icon: 'none' });
      setTimeout(() => wx.navigateBack(), 1500);
    }
  },

  // 加载预约详情
  loadOrderDetail(id) {
    this.setData({ loading: true });
    
    request({
      url: `/api/reservation/${id}`,
      method: 'GET'
    }).then(data => {
      this.setData({ loading: false });
      this.setOrderData(data);
    }).catch(err => {
      this.setData({ loading: false });
      wx.showToast({ title: err.message || '加载失败', icon: 'none' });
      setTimeout(() => wx.navigateBack(), 1500);
    });
  },

  setOrderData(order) {
    // 状态映射
    const statusMap = {
      1: { text: '待签到', icon: '⏰', color: '#ff9800' },
      2: { text: '已签到', icon: '✅', color: '#4caf50' },
      3: { text: '已取消', icon: '❌', color: '#999' },
      4: { text: '自动取消', icon: '⚠️', color: '#f44336' }
    };
    const status = statusMap[order.status] || { text: '未知', icon: '❓', color: '#999' };

    // 计算签到截止时间
    let checkinDeadline = '';
    if (order.status === 1 && order.startTime) {
      const [hour, minute] = order.startTime.split(':');
      let deadlineHour = parseInt(hour);
      let deadlineMinute = parseInt(minute) + 15;
      if (deadlineMinute >= 60) {
        deadlineHour += 1;
        deadlineMinute -= 60;
      }
      checkinDeadline = `${String(deadlineHour).padStart(2, '0')}:${String(deadlineMinute).padStart(2, '0')}`;
    }

    // 格式化时间
    let createTimeText = '--';
    if (order.createTime) {
      const date = new Date(order.createTime);
      createTimeText = `${date.getMonth()+1}/${date.getDate()} ${String(date.getHours()).padStart(2,'0')}:${String(date.getMinutes()).padStart(2,'0')}`;
    }

    this.setData({
      order: {
        id: order.id,
        reserveNo: order.reserveNo,
        seatCode: order.seatCode,
        roomName: order.roomName,
        date: order.date,
        startTime: order.startTime,
        endTime: order.endTime,
        status: order.status,
        statusText: status.text,
        statusColor: status.color,
        createTimeText: createTimeText
      },
      statusIcon: status.icon,
      checkinDeadline: checkinDeadline
    });
  },

  // 去签到
  goCheckin() {
    const { order } = this.data;
    if (order.status !== 1) {
      wx.showToast({ title: '该状态无法签到', icon: 'none' });
      return;
    }
    wx.navigateTo({
      url: `/pages/checkin/checkin?id=${order.id}`
    });
  },

  // 取消预约
  cancelOrder() {
    const { order } = this.data;
    
    if (order.status !== 1) {
      wx.showToast({ title: '该状态无法取消', icon: 'none' });
      return;
    }
    
    wx.showModal({
      title: '确认取消',
      content: '取消后该座位将释放给其他同学，确定取消吗？',
      confirmColor: '#ff4d4f',
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
            
            // 更新页面状态
            this.setData({
              'order.status': 3,
              'order.statusText': '已取消',
              'order.statusColor': '#999'
            });
            
            // 通知上一页刷新
            const pages = getCurrentPages();
            const prevPage = pages[pages.length - 2];
            if (prevPage && prevPage.loadOrders) {
              prevPage.loadOrders();
            }
          }).catch(err => {
            wx.hideLoading();
            wx.showToast({ title: err.message || '取消失败', icon: 'none' });
          });
        }
      }
    });
  },

  // 重新预约
  rebook() {
    wx.switchTab({ url: '/pages/seat/seat' });
  },

  // 分享
  onShareAppMessage() {
    return {
      title: `我的自习室预约 - ${this.data.order.seatCode || ''}号座位`,
      path: `/pages/reservation-detail/reservation-detail?id=${this.data.order.id}`
    };
  }
});