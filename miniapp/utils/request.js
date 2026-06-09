const app = getApp();

// 是否正在刷新Token
let isRefreshing = false;
// 等待队列
let refreshSubscribers = [];

function onRefreshed(token) {
  refreshSubscribers.forEach(cb => cb(token));
  refreshSubscribers = [];
}

function clearLoginAndRedirect() {
  wx.removeStorageSync('token');
  wx.removeStorageSync('refreshToken');
  wx.removeStorageSync('userInfo');
  wx.showToast({ title: '登录已过期，请重新登录', icon: 'none' });
  setTimeout(() => {
    wx.reLaunch({ url: '/pages/login/login' });
  }, 1500);
}

const request = (options) => {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token');
    const buildHeader = (authToken) => ({
      'Content-Type': 'application/json',
      'Authorization': authToken ? `Bearer ${authToken}` : ''
    });

    const retryRequest = (newToken) => {
      wx.request({
        url: app.globalData.baseUrl + options.url,
        method: options.method || 'GET',
        data: options.data || {},
        header: buildHeader(newToken),
        success: (retryRes) => {
          if (retryRes.statusCode === 200 && retryRes.data.code === 200) {
            resolve(retryRes.data.data);
          } else {
            reject(retryRes.data || { message: '请求失败' });
          }
        },
        fail: (err) => reject(err)
      });
    };
    
    wx.request({
      url: app.globalData.baseUrl + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: buildHeader(token),
      success: (res) => {
        if (res.statusCode === 200 && res.data.code === 200) {
          resolve(res.data.data);
        } else if (res.statusCode === 401 || (res.data && res.data.code === 401)) {
          // Token过期，尝试刷新
          const refreshToken = wx.getStorageSync('refreshToken');
          
          if (!refreshToken) {
            clearLoginAndRedirect();
            reject({ message: '登录已过期' });
            return;
          }
          
          // 如果正在刷新，加入等待队列
          if (isRefreshing) {
            refreshSubscribers.push((newToken) => {
              retryRequest(newToken);
            });
            return;
          }
          
          isRefreshing = true;
          
          // 调用刷新Token接口
          wx.request({
            url: app.globalData.baseUrl + '/api/auth/refresh',
            method: 'POST',
            data: { refreshToken },
            success: (refreshRes) => {
              if (refreshRes.statusCode === 200 && refreshRes.data.code === 200) {
                const newToken = refreshRes.data.data.accessToken;
                const newRefreshToken = refreshRes.data.data.refreshToken;
                
                wx.setStorageSync('token', newToken);
                wx.setStorageSync('refreshToken', newRefreshToken);
                
                onRefreshed(newToken);
                
                // 重试当前请求
                retryRequest(newToken);
              } else {
                clearLoginAndRedirect();
                reject({ message: '登录已过期' });
              }
              isRefreshing = false;
            },
            fail: () => {
              clearLoginAndRedirect();
              reject({ message: '网络错误' });
              isRefreshing = false;
            }
          });
        } else {
          reject(res.data || { message: '请求失败' });
        }
      },
      fail: (err) => {
        console.error('请求失败:', err);
        reject({ message: '网络错误，请稍后重试' });
      }
    });
  });
};

module.exports = request;
