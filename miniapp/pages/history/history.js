const request = require('../../utils/request.js');

Page({
  data: {
    historyOrders: [],
    loading: false,
    page: 1,
    pageSize: 20,
    hasMore: true,
    total: 0
  },

  onShow() {
    this.setData({ page: 1, historyOrders: [], hasMore: true });
    this.loadHistoryOrders();
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadHistoryOrders();
    }
  },

  onPullDownRefresh() {
    this.setData({ page: 1, historyOrders: [], hasMore: true });
    this.loadHistoryOrders(true);
  },

  loadHistoryOrders(isPullDown = false) {
    const token = wx.getStorageSync('token');
    if (!token) {
      this.setData({ historyOrders: [], loading: false });
      if (isPullDown) wx.stopPullDownRefresh();
      return;
    }
    
    this.setData({ loading: true });
    
    // 获取最近90天的历史记录
    const endDate = new Date();
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - 90);
    
    const formatDate = (date) => {
      return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
    };
    
    request({
      url: '/api/reservation/history',
      method: 'GET',
      data: {
        startDate: formatDate(startDate),
        endDate: formatDate(endDate),
        page: this.data.page,
        size: this.data.pageSize
      }
    }).then(data => {
      const records = data.records || [];
      const formattedRecords = records.map(this.formatOrder);
      
      this.setData({
        historyOrders: this.data.page === 1 ? formattedRecords : [...this.data.historyOrders, ...formattedRecords],
        loading: false,
        hasMore: records.length === this.data.pageSize,
        total: data.total || 0,
        page: this.data.page + 1
      });
      
      if (isPullDown) {
        wx.stopPullDownRefresh();
      }
    }).catch(err => {
      console.error('加载历史记录失败:', err);
      this.setData({ loading: false });
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
    
    // 格式化时间
    let useTime = '';
    if (order.status === 2 && order.signTime) {
      const date = new Date(order.signTime);
      useTime = `${date.getMonth()+1}/${date.getDate()} ${String(date.getHours()).padStart(2,'0')}:${String(date.getMinutes()).padStart(2,'0')}`;
    } else if (order.status === 3 && order.cancelTime) {
      const date = new Date(order.cancelTime);
      useTime = `${date.getMonth()+1}/${date.getDate()} ${String(date.getHours()).padStart(2,'0')}:${String(date.getMinutes()).padStart(2,'0')}`;
    }
    
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
      statusColor: status.color,
      useTime: useTime
    };
  },

  // 删除历史记录
  deleteHistory(e) {
    const order = e.currentTarget.dataset.order;
    
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这条历史记录吗？',
      success: (res) => {
        if (res.confirm) {
          // 注意：后端可能需要提供删除接口，如果没有则只在前端删除
          // 这里先做前端删除，等后端有接口再改
          let history = this.data.historyOrders;
          const index = history.findIndex(item => item.id === order.id);
          if (index !== -1) {
            history.splice(index, 1);
            this.setData({ historyOrders: history });
            wx.showToast({ title: '已删除', icon: 'success' });
          }
          
          // TODO: 如果有后端删除接口，调用它
          // request({
          //   url: '/api/reservation/delete',
          //   method: 'POST',
          //   data: { reservationId: order.id }
          // }).then(() => { ... })
        }
      }
    });
  },

  // 重新预约
  rebook(e) {
    const order = e.currentTarget.dataset.order;
    wx.switchTab({ url: '/pages/seat/seat' });
  },

  // 查看详情
  viewDetail(e) {
    const order = e.currentTarget.dataset.order;
    wx.navigateTo({
      url: `/pages/reservation-detail/reservation-detail?id=${order.id}`
    });
  }
});