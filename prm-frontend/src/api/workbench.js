import request from './request'

// 统一待办中心:跨域汇聚确权/授权/风险待办(eLink 待办的 Web 实现,台账服务为枢纽)
export const getTodos = () => request.get('/dpr/ledger/todo')
