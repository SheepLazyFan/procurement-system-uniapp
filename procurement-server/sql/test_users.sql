SET NAMES utf8mb4;

-- 创建三个员工测试用户
INSERT INTO sys_user (nick_name, role, enterprise_id, wx_openid) VALUES
  ('测试管理员', 'MEMBER', 1, 'test_openid_admin'),
  ('测试销售员', 'MEMBER', 1, 'test_openid_sales'),
  ('测试仓库员', 'MEMBER', 1, 'test_openid_warehouse');

-- 获取刚插入的第一个用户ID
SET @first_id = LAST_INSERT_ID();

-- 创建团队成员记录
INSERT INTO sys_team_member (enterprise_id, user_id, role, permissions) VALUES
  (1, @first_id,     'ADMIN',     '{"inventory":true,"order":true,"statistics":true}'),
  (1, @first_id + 1, 'SALES',     '{"inventory":false,"order":true,"statistics":false}'),
  (1, @first_id + 2, 'WAREHOUSE', '{"inventory":true,"order":false,"statistics":false}');

-- 显示结果
SELECT u.id AS user_id, u.nick_name, u.role AS user_role, u.wx_openid, u.enterprise_id, t.role AS team_role, t.permissions
FROM sys_user u
LEFT JOIN sys_team_member t ON u.id = t.user_id AND t.is_deleted=0
WHERE u.is_deleted=0
ORDER BY u.id;
