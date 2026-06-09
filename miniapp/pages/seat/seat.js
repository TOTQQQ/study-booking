const request = require('../../utils/request.js');

Page({
  data: {
    selectDate: "",
    studyRoomList: [
      { id: 1, roomName: '一楼自习室' },
      { id: 2, roomName: '二楼自习室' },
      { id: 3, roomName: '三楼自习室' }
    ],
    selectRoomId: 1,
    selectRoomName: "一楼自习室",
    seatList: [],
    timePeriodList: [],
    selectSeat: null,
    selectTimePeriodId: null,
    showPopup: false,
    loading: false,
    submitting: false
  },

  onLoad() {
    this.initDate();
    this.initTimePeriodList();
    this.loadSeatStatus();
  },

  onShow() {
    if (!wx.getStorageSync('token')) {
      wx.reLaunch({ url: '/pages/login/login' });
      return;
    }
    if (this.data.selectRoomId) {
      this.loadSeatStatus();
    }
  },

  initDate() {
    const date = new Date();
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');
    this.setData({ selectDate: `${year}-${month}-${day}` });
  },

  // 初始化时间段：ID需和数据库 timePeriod 初始化数据一致
  initTimePeriodList() {
    const list = [
      { id: 1, startTime: '08:00', endTime: '10:00' },
      { id: 2, startTime: '10:00', endTime: '12:00' },
      { id: 3, startTime: '13:00', endTime: '15:00' },
      { id: 4, startTime: '15:00', endTime: '17:00' },
      { id: 5, startTime: '18:00', endTime: '20:00' },
      { id: 6, startTime: '20:00', endTime: '22:00' }
    ];
    this.setData({ timePeriodList: list });
  },

  // 查询座位状态（调用后端）
  loadSeatStatus() {
    const { selectRoomId, selectDate } = this.data;
    if (!selectRoomId) return;
    
    this.setData({ loading: true });
    
    request({
      url: '/api/seat/query',
      method: 'POST',
      data: {
        roomId: selectRoomId,
        date: selectDate
      }
    }).then(data => {
      this.setData({ loading: false });
      
      const seatList = (data || []).map(seat => {
        let isUsed = false;
        if (this.data.selectTimePeriodId && seat.timeSlots) {
          const slot = seat.timeSlots.find(s => s.timePeriodId === this.data.selectTimePeriodId);
          isUsed = slot ? slot.status === 2 : false;
        }
        
        return {
          id: seat.id,
          seatCode: seat.seatCode,
          isUsed: isUsed,
          timeSlots: seat.timeSlots || []
        };
      });
      
      this.setData({ seatList });
    }).catch(err => {
      this.setData({ loading: false });
      console.error('查询座位失败:', err);
      wx.showToast({ title: err.message || '查询失败', icon: 'none' });
    });
  },

  // 切换日期
  bindDateChange(e) {
    this.setData({ selectDate: e.detail.value });
    this.loadSeatStatus();
  },

  // 切换自习室
  bindRoomChange(e) {
    const index = e.detail.value;
    const room = this.data.studyRoomList[index];
    this.setData({
      selectRoomId: room.id,
      selectRoomName: room.roomName,
      selectTimePeriodId: null
    });
    this.loadSeatStatus();
  },

  // 切换时段（筛选显示）- 修复：索引转ID
  bindTimePeriodChange(e) {
    const index = parseInt(e.detail.value);
    const timePeriodId = this.data.timePeriodList[index].id;
    this.setData({ selectTimePeriodId: timePeriodId });
    this.updateSeatUsedStatus();
  },

  // 根据选中时段更新座位占用状态
  updateSeatUsedStatus() {
    const { seatList, selectTimePeriodId } = this.data;
    const newList = seatList.map(seat => {
      let isUsed = false;
      if (selectTimePeriodId && seat.timeSlots) {
        const slot = seat.timeSlots.find(s => s.timePeriodId === selectTimePeriodId);
        isUsed = slot ? slot.status === 2 : false;
      }
      return { ...seat, isUsed };
    });
    this.setData({ seatList: newList });
  },

  // 选择座位
  chooseSeat(e) {
    const seat = e.currentTarget.dataset.seat;
    if (seat.isUsed) {
      wx.showToast({ title: '该时段已被预约', icon: 'none' });
      return;
    }
    this.setData({
      selectSeat: seat,
      showPopup: true,
      selectTimePeriodId: null
    });
  },

  // 在弹窗中选择时段
  selectTimePeriod(e) {
    const timeId = e.currentTarget.dataset.time.id;
    this.setData({ selectTimePeriodId: timeId });
  },

  // 检查时段是否可预约（弹窗中用）
  isTimePeriodAvailable(timeId) {
    const { selectSeat } = this.data;
    if (!selectSeat || !selectSeat.timeSlots) return true;
    const slot = selectSeat.timeSlots.find(s => s.timePeriodId === timeId);
    return slot ? slot.status !== 2 : true;
  },

  // 检查时段是否可预约（调用后端实时检查）
  checkTimePeriodAvailable(timeId) {
    const { selectSeat, selectDate } = this.data;
    if (!selectSeat) return Promise.resolve(true);
    
    return request({
      url: '/api/reservation/check',
      method: 'GET',
      data: {
        seatId: selectSeat.id,
        date: selectDate,
        timePeriodId: timeId
      }
    }).then(available => {
      return available;
    }).catch(() => {
      return true;
    });
  },

  // 确认预约（调用后端）
  confirmSelect() {
    const { selectSeat, selectDate, selectTimePeriodId } = this.data;
    
    if (!selectTimePeriodId) {
      wx.showToast({ title: '请选择时段', icon: 'none' });
      return;
    }
    
    this.setData({ submitting: true });
    
    this.checkTimePeriodAvailable(selectTimePeriodId).then(available => {
      if (!available) {
        this.setData({ submitting: false });
        wx.showToast({ title: '座位已被其他人预约', icon: 'none' });
        this.loadSeatStatus();
        return;
      }
      
      return request({
        url: '/api/reservation/reserve',
        method: 'POST',
        data: {
          seatId: selectSeat.id,
          date: selectDate,
          timePeriodId: selectTimePeriodId
        }
      });
    }).then(data => {
      this.setData({ submitting: false });
      wx.showToast({ title: '预约成功', icon: 'success' });
      
      setTimeout(() => {
        this.setData({ 
          showPopup: false, 
          selectSeat: null,
          selectTimePeriodId: null 
        });
        this.loadSeatStatus();
      }, 1500);
    }).catch(err => {
      this.setData({ submitting: false });
      wx.showToast({ title: err.message || '预约失败', icon: 'none' });
    });
  },

  // 关闭弹窗
  closePopup() {
    this.setData({ 
      showPopup: false, 
      selectSeat: null,
      selectTimePeriodId: null 
    });
  },

  // 获取选中时段显示文本
  getSelectedTimeRange() {
    const { timePeriodList, selectTimePeriodId } = this.data;
    const selected = timePeriodList.find(t => t.id === selectTimePeriodId);
    if (!selected) return '';
    return `${selected.startTime}-${selected.endTime}`;
  },

  // 阻止冒泡
  stopPropagation() {}
});
