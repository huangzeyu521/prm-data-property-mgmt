import { test, expect } from '@playwright/test'

/**
 * review(合规管控小组)从「协议工作台」菜单移除的核验:review 侧栏不再有协议工作台入口,
 * 其余授权审核相关入口仍在(批量授权清单/合规校验/授权审核提交)。须 PW_USER=review。
 */
test('review 侧栏无「协议工作台」入口,授权审核类入口仍在', async ({ page }) => {
  await page.goto('/dpr/workbench/todo', { waitUntil: 'networkidle' })
  // 展开「数据授权管理」分组
  const grp = page.locator('.el-sub-menu__title', { hasText: '数据授权管理' }).first()
  if (await grp.isVisible().catch(() => false)) await grp.click()
  await page.waitForTimeout(300)
  // 协议工作台入口应消失(菜单项不含该标题)
  await expect(page.locator('.el-menu-item').filter({ hasText: '协议工作台' })).toHaveCount(0)
  // review 仍应看到合规审核相关入口
  await expect(page.locator('.el-menu-item').filter({ hasText: '合规校验管理' }).first()).toBeVisible()
})
