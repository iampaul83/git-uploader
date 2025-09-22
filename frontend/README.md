# Frontend

此專案為 Azure DevOps Repo Uploader 的 Angular 20 前端。介面提供 PAT 設定、Repo 匯入與 commit/push 操作，所有請求皆透過 `/api` 傳送至 Spring Boot 後端。

## 開發流程

```bash
npm install
npm start
```

啟動後造訪 `http://localhost:4200`，Angular 開發伺服器會依據 `proxy.conf.json` 將 `/api` 代理到 `http://localhost:8080`。

## 建置專案

```bash
npm run build
```

打包結果位於 `dist/` 目錄，可佈署於任何靜態伺服器。
