App({
  globalData: {
    baseUrl: "http://localhost:8080",  // 改成你的后端地址
    userToken: "",
    userInfo: null
  },

  onLaunch() {
    console.log("共享自习室座位预约小程序启动");
  }
});