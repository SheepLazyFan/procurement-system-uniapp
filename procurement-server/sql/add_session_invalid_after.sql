ALTER TABLE `sys_enterprise`
  ADD COLUMN `session_invalid_after` DATETIME DEFAULT NULL
  COMMENT '企业会话失效时间，恢复等高风险操作后用于强制重新登录'
  AFTER `payment_qr_url`;
