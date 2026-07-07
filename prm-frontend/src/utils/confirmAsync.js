import { ElMessageBox } from 'element-plus'

// 危险操作二次确认 + 提交中态反馈(Element MessageBox 官方 beforeClose+confirmButtonLoading 模式,
// 对齐 DUI 组件规范:MessageBox 是唯一支持"确认时显示 loading"的弹窗组件)。
// action 应返回 Promise;确认后进入 loading 态直至 action 完成才关闭弹窗,避免网络延迟期间重复点击。
// 用户取消(cancel/close)按原样 reject,调用方仍需自行 .catch(() => {}) 吞掉。
export function confirmAsync(message, title, action, opts = {}) {
  return ElMessageBox.confirm(message, title, {
    type: 'warning',
    ...opts,
    beforeClose: async (act, instance, done) => {
      if (act !== 'confirm') { done(); return }
      instance.confirmButtonLoading = true
      const originalText = instance.confirmButtonText
      instance.confirmButtonText = '处理中...'
      try {
        await action()
        done()
      } catch (e) {
        // 失败已由请求拦截器 toast;不关闭弹窗,允许用户查看后重试或取消
      } finally {
        instance.confirmButtonLoading = false
        instance.confirmButtonText = originalText
      }
    }
  })
}
