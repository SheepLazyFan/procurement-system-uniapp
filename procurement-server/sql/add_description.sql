-- 本地执行：为 pms_product 表添加 description 字段
ALTER TABLE pms_product 
ADD COLUMN description TEXT DEFAULT NULL COMMENT '商品描述（图文介绍）' AFTER qrcode_image;
