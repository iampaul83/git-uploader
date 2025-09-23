# Azure DevOps Repo Uploader

這是一個專為個人使用而設計的簡化 Git 操作工具，後端使用 Spring Boot 3、前端使用 Angular 20。系統專注於協助管理 Azure DevOps Repos，提供 PAT 設定、快速 clone、以及針對指定資料夾的 commit / push 流程。

## 功能總覽

- **PAT 設定**：透過網頁介面保存 Azure DevOps Personal Access Token（儲存在 `backend/data/pat.txt`）。
- **Repo 清單**：列出專案根目錄下 `repos/` 資料夾內已 clone 的 repository。
- **新增 Repo**：輸入 Azure DevOps repo URL 與分支；系統會盡力從 URL 推斷分支名稱，並確保 `git clone` 使用 `--depth 1`、HTTP/1.1 以及停用 LFS smudge。
- **自動建置結構**：clone 完成後若 repo 中不存在 `<當前年份>/<branch_name>` 目錄，會自動建立。
- **Commit & Push**：只會針對 `<當前年份>/<branch_name>` 目錄進行 `git add` / `commit`，並在推送前自動 fast-forward 遠端更新。

## 環境需求

- Java 21+
- Node.js 20+
- Git 2.40+

## 專案結構

```
backend/   # Spring Boot 3 REST API
frontend/  # Angular 20 前端介面
repos/     # 由系統建立，存放 clone 下來的 repo
```

## 建置 Jar（包含前端與後端）

專案根目錄提供 `build.sh` 指令稿，會自動安裝前端依賴、建置 Angular 靜態檔並打包至 Spring Boot 可執行 Jar 中，同時在輸出目錄產生可供使用者調整的設定檔。

```bash
./build.sh
```

成功後會在 `build/` 資料夾得到：

- `git-uploader.jar`：整合前端與後端的執行檔。
- `config/application.properties`：外部設定檔，可於啟動前自行調整連線埠、日誌等設定。

啟動方式：

```bash
cd build
java -jar git-uploader.jar
```

啟動後瀏覽器開啟 `http://localhost:8080` 即可使用前端介面，後端 API 會以 `/api` 作為前綴。

## 直接啟動後端（開發模式）

若僅需在開發時啟動後端服務，可照以下方式啟動：

```bash
cd backend
./mvnw spring-boot:run
```

後端服務啟動後提供以下重點 API：

- `POST /api/pat`：儲存 PAT。
- `GET /api/pat`：檢查 PAT 是否已設定（回傳遮罩後的內容）。
- `POST /api/repos`：新增 repo。
- `POST /api/repos/{repoId}/commit-and-push`：對指定 repo 執行 commit & push。
- `GET /api/repos`：列出所有 repo 摘要資訊。

`git` 指令在執行時會透過 `backend/data/git-askpass.sh` 取得 PAT，採非互動式流程。

## 啟動前端（開發模式）

```bash
cd frontend
npm install
npm start
```

開發模式下的 Angular 仍會運作於 `http://localhost:4200`，並透過 `proxy.conf.json` 將 `/api` 代理至 `http://localhost:8080`。

## 使用流程

1. 先在介面上輸入具備 Repo 讀寫權限的 PAT。
2. 新增 Azure DevOps repo：
   - 若 URL 帶有 `?version=GBxxxx` 參數會自動抓取分支。
   - 若遠端不存在該分支，會從 `master` 建立並推送。
   - clone 目標資料夾格式為 `project_repository_branch`，統一放在 `repos/` 下。
3. 在 repo 中編輯 `<當前年份>/<branch_name>` 資料夾。
4. 於前端輸入 commit 訊息後按下「Commit 並 Push」。
   - 系統會先 `git fetch` 並 fast-forward。
   - 僅會提交指定資料夾的變更。
   - 成功後自動推送到遠端分支。

## 測試

- 後端：`cd backend && ./mvnw test`
- 前端：`cd frontend && npm run build`

## 注意事項

- PAT 會以明文儲存在 `backend/data/pat.txt`，請妥善保管本機環境。
- 若 repo 為私有並啟用了 Git LFS，系統預設會設定 `lfs.skipSmudge=true` 以避免自動下載大檔案。
- `commit-and-push` API 僅會處理 `<當前年份>/<branch_name>` 目錄，請將要上傳的檔案放在此路徑底下。
- Jar 會優先讀取與其同層的 `config/application.properties`，可依需求修改後再啟動服務。
