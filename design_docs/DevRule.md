# 前端代码开发规范
1. 文件命名
页面/组件：大驼峰 GoodList.tsx
工具/状态文件：小驼峰 request.ts

2. TS类型规范
所有函数、对象必须定义类型，禁止滥用any

3. 分层规范
通用组件放入src/components，业务页面放入src/pages
所有接口请求统一使用utils/request.ts，不新建axios

4. 样式规范
统一使用antd内置样式，自定义样式统一管理