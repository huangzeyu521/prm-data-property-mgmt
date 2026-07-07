/*
 * Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
 * 来源识别(A–F) / 信息关联(G–J) 代码释义 —— 单一真源。
 * 口径对齐《表1 数据确权信息清单(系统级)》:数据来源权益识别 A–F、信息关联权益识别 G–J。
 * 供确权申请查询、向导等处复用,避免各页面各自维护导致漂移。
 */

export const SOURCE_CODES = [
  { v: 'A', label: '自行生产数据' },
  { v: 'B', label: '公开采集数据' },
  { v: 'C', label: '公共数据授权' },
  { v: 'D', label: '共同生产数据' },
  { v: 'E', label: '交易采购数据' },
  { v: 'F', label: '其他方式获取' }
]

export const RELATION_CODES = [
  { v: 'G', label: '涉及行政监管要求' },
  { v: 'H', label: '涉及用户个人/家庭隐私' },
  { v: 'I', label: '涉及第三方商业机密' },
  { v: 'J', label: '其他第三方机构协议' }
]

export const SOURCE_MAP = Object.fromEntries(SOURCE_CODES.map(o => [o.v, o.label]))
export const RELATION_MAP = Object.fromEntries(RELATION_CODES.map(o => [o.v, o.label]))

/** 代码 → "A 自行生产数据"(带释义,找不到时回退原码)。 */
export const identText = (code, map) => `${code} ${map[code] || ''}`.trim()
