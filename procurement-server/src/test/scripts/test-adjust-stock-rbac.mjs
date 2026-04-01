/**
 * T-08 ~ T-11：adjustStock RBAC 权限 API 测试
 *
 * 测试 PUT /products/{id}/stock 端点的 @PreAuthorize("hasAnyRole('SELLER','ADMIN','WAREHOUSE')")
 *   T-08: SELLER    → 200 (允许)
 *   T-09: ADMIN     → 200 (允许)
 *   T-10: WAREHOUSE → 200 (允许)
 *   T-11: SALES     → 403 (拒绝)
 *
 * 运行方式：node test-adjust-stock-rbac.mjs [baseUrl]
 * 默认 baseUrl: http://106.52.136.176:8082
 *
 * 依赖：Node.js 18+（内置 fetch、crypto）
 */

import crypto from 'crypto';

// ======================== 配置 ========================
const BASE_URL = process.argv[2] || 'http://106.52.136.176:8082';
const JWT_SECRET = 'f5d97624d90bd4489b3b345a89af218e0ed389952959bd590614e304da8e2559';
const ENTERPRISE_ID = 1;
const PRODUCT_ID = 1; // 用一个真实存在的商品ID

// ======================== JWT 工具 ========================

function base64url(str) {
  return Buffer.from(str)
    .toString('base64')
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
}

function signJwt(payload) {
  const header = { alg: 'HS256', typ: 'JWT' };
  const headerB64 = base64url(JSON.stringify(header));
  const payloadB64 = base64url(JSON.stringify(payload));
  const signature = crypto
    .createHmac('sha256', Buffer.from(JWT_SECRET, 'utf-8'))
    .update(`${headerB64}.${payloadB64}`)
    .digest('base64')
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
  return `${headerB64}.${payloadB64}.${signature}`;
}

function generateToken(userId, role, enterpriseId) {
  const now = Math.floor(Date.now() / 1000);
  return signJwt({
    sub: String(userId),
    phone: '13800000001',
    role: role,
    enterpriseId: enterpriseId,
    iat: now,
    exp: now + 3600, // 1小时有效
  });
}

// ======================== 测试逻辑 ========================

/**
 * 用指定 role 的 token 调用 PUT /products/{id}/stock
 * 使用 IN(入库) +0 的方式，不会影响真实库存
 * 注意：quantity >= 1，所以会实际入库 1 件，测试完后需要手动调回
 */
async function testAdjustStock(testId, role, expectedStatus) {
  // 对于 SELLER 角色，JWT role claim 是 "SELLER"
  // 对于团队成员角色(ADMIN/SALES/WAREHOUSE)，JWT role claim 是 "MEMBER"
  // JwtAuthenticationFilter 会根据 role claim 和 memberRole 来构造 LoginUser
  // 但 JWT token 中 role 字段存的是顶级角色（SELLER/MEMBER/BUYER）
  // memberRole 是从 team_members 表查出来的
  //
  // 因此我们不能简单地用 JWT 伪造不同角色来测试
  // 需要用真实用户的 JWT token

  const token = generateToken(testId + 100, role, ENTERPRISE_ID);

  const body = JSON.stringify({
    productId: PRODUCT_ID,
    quantity: 1,
    type: 'IN',
  });

  try {
    const resp = await fetch(`${BASE_URL}/products/${PRODUCT_ID}/stock`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body,
    });

    const status = resp.status;
    const result = status === expectedStatus ? '✅ PASS' : '❌ FAIL';
    const respText = await resp.text().catch(() => '');

    console.log(
      `  ${testId}: role=${role.padEnd(10)} expected=${expectedStatus} actual=${status} ${result}`
    );
    if (status !== expectedStatus) {
      console.log(`       Response: ${respText.substring(0, 200)}`);
    }
    return status === expectedStatus;
  } catch (err) {
    console.log(`  ${testId}: role=${role.padEnd(10)} ❌ ERROR: ${err.message}`);
    return false;
  }
}

// ======================== 主流程 ========================

console.log('');
console.log('=== T-08~T-11: adjustStock RBAC 权限测试 ===');
console.log(`目标: ${BASE_URL}/products/${PRODUCT_ID}/stock`);
console.log('');
console.log('⚠️  注意: 此脚本生成的 JWT token 中 role 字段直接设为对应角色名。');
console.log('   如果 JwtAuthenticationFilter 从 team_members 表读取 memberRole，');
console.log('   则需要使用真实登录的 token 来测试。');
console.log('   下方提供了使用真实 token 的替代测试方式。');
console.log('');

// 方案一：直接用伪造的 JWT (如果 filter 直接使用 role claim)
console.log('--- 方案一：JWT role claim 直接测试 ---');

const results = [];
results.push(await testAdjustStock('T-08', 'SELLER', 200));
results.push(await testAdjustStock('T-09', 'ADMIN', 200));
results.push(await testAdjustStock('T-10', 'WAREHOUSE', 200));
results.push(await testAdjustStock('T-11', 'SALES', 403));

const passed = results.filter(Boolean).length;
const total = results.length;

console.log('');
console.log(`结果：${passed}/${total} 通过`);
console.log('');

// 方案二：提供 curl 命令模板，用真实 token 测试
console.log('--- 方案二：使用真实登录 token 的 curl 命令 ---');
console.log('');
console.log('如果方案一因为 JWT filter 链的限制不准确，请用以下步骤手动测试：');
console.log('');
console.log('1. 用 SELLER 账号登录，获取 token，替换 <SELLER_TOKEN>');
console.log('2. 用 SALES 团队成员登录，获取 token，替换 <SALES_TOKEN>');
console.log('');
console.log(`curl -X PUT "${BASE_URL}/products/${PRODUCT_ID}/stock" \\`);
console.log('  -H "Content-Type: application/json" \\');
console.log('  -H "Authorization: Bearer <SELLER_TOKEN>" \\');
console.log(`  -d '{"productId":${PRODUCT_ID},"quantity":1,"type":"IN"}'`);
console.log('  # 预期: 200');
console.log('');
console.log(`curl -X PUT "${BASE_URL}/products/${PRODUCT_ID}/stock" \\`);
console.log('  -H "Content-Type: application/json" \\');
console.log('  -H "Authorization: Bearer <SALES_TOKEN>" \\');
console.log(`  -d '{"productId":${PRODUCT_ID},"quantity":1,"type":"IN"}'`);
console.log('  # 预期: 403');

process.exit(passed === total ? 0 : 1);
