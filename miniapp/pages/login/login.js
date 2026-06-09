const request = require('../../utils/request.js');

Page({
  data: {
    canIUseGetUserProfile: false
  },

  onLoad() {
    // 检查是否支持getUserProfile
    if (wx.getUserProfile) {
      this.setData({ canIUseGetUserProfile: true });
    }
  },

  onShow() {
    // 已登录直接跳转
    if (wx.getStorageSync('token')) {
      this.goToSeat();
    }
  },

  // 微信授权登录
  handleLogin() {
    wx.getUserProfile({
      desc: '用于完善用户资料',
      success: (userRes) => {
        this.doLogin(userRes.userInfo);
      },
      fail: () => {
        wx.showToast({ title: '需要授权才能登录', icon: 'none' });
      }
    });
  },

  doLogin(userInfo) {
    wx.showLoading({ title: '登录中...', mask: true });
    
    // 获取微信code
    wx.login({
      success: (loginRes) => {
        if (!loginRes.code) {
          wx.hideLoading();
          wx.showToast({ title: '获取授权失败', icon: 'none' });
          return;
        }
        
        // 调用后端登录接口
        request({
          url: '/api/auth/login',
          method: 'POST',
          data: {
            code: loginRes.code,
            nickname: userInfo.nickName,
            avatar: userInfo.avatarUrl
          }
        }).then(data => {
          wx.hideLoading();
          
          // 保存登录信息
          wx.setStorageSync('token', data.accessToken);
          wx.setStorageSync('refreshToken', data.refreshToken);
          wx.setStorageSync('userInfo', {
            userId: data.userId,
            nickname: data.nickname,
            avatar: data.avatar,
            isNewUser: data.isNewUser
          });
          
          wx.showToast({ title: '登录成功', icon: 'success' });
          
          setTimeout(() => {
            this.goToSeat();
          }, 1000);
        }).catch(err => {
          wx.hideLoading();
          wx.showToast({ title: err.message || '登录失败', icon: 'none' });
        });
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '获取微信授权失败', icon: 'none' });
      }
    });
  },

  goToSeat() {
    wx.switchTab({ url: '/pages/seat/seat' });
  }
});