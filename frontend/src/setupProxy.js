const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function (app) {
  app.use(
    '/mock-pg',
    createProxyMiddleware({
      target: 'https://mock-pg-1046420547293.us-central1.run.app',
      changeOrigin: true,
      pathRewrite: { '^/mock-pg': '' },
    })
  );
};
