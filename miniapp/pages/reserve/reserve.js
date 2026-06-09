Page({
  data: {
    seat: "",
    date: "",
    time: ""
  },

  onLoad(options) {
    this.setData({
      seat: options.seat,
      date: options.date,
      time: options.time
    });
  },

  // 提交预约
  submitReserve() {
    const { seat, date, time } = this.data;

    // 构造预约数据
    const order = {
      seat: seat,
      date: date,
      time: time,
      status: "待签到",
      createTime: new Date().getTime()
    };

    // 读取本地已有预约
    let orders = wx.getStorageSync("validOrders") || [];

    // 去重：同一个座位+日期+时段不能重复预约
    const hasOrder = orders.some(item => {
      return item.seat == seat && item.date == date && item.time == time;
    });

    if (hasOrder) {
      wx.showToast({
        title: "该时段已预约",
        icon: "none"
      });
      return;
    }

    // 加入新预约
    orders.push(order);
    wx.setStorageSync("validOrders", orders);

    wx.showToast({
      title: "预约成功"
    });

    setTimeout(() => {
      wx.navigateBack();
    }, 1500);
  }
});