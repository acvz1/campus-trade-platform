# Git团队协作规范
1. 分支划分
main：线上稳定分支，禁止直接提交代码
dev：开发主分支，所有功能合并到此
feature/xxx：新功能分支，例 feature/publish-goods

2. 提交注释格式
feat: 新增功能
fix: 修复bug
docs: 修改文档规范
style: 调整样式

3. 开发流程
1. git pull 拉取dev最新代码
2. git checkout -b feature/功能名 创建分支开发
3. git add . && git commit -m "注释内容"
4. git push 推送远程
5. 提交合并请求到dev，审核后合并