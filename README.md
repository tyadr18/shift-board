# shift-board

A small shift-request board built with Spring Boot 3.5 and Thymeleaf: staff submit, filter, edit, and delete their preferred shifts behind a simple form login.
This is a portfolio sample with fictional demo data only.

[![test](https://github.com/tyadr18/shift-board/actions/workflows/test.yml/badge.svg)](https://github.com/tyadr18/shift-board/actions/workflows/test.yml)

## 概要

スタッフが「この日のこの時間帯に入りたい」というシフト希望を登録し、一覧で確認するための小さな Web アプリです。ポートフォリオ用の作例で、実在するクライアントの案件ではありません。サンプルデータの名前（スタッフA など）はすべて架空のものです。

できること:

- シフト希望の登録・一覧・編集・削除（項目: スタッフ名、日付、時間帯、備考）
- 一覧の絞り込み（スタッフ名の部分一致、期間の開始日・終了日）
- フォームログイン（Spring Security）。ログインしないと画面を開けません
- 入力チェック（必須、文字数、日付の形式、開始日と終了日の前後関係）
- 業務ルールのチェック（過去の日付は不可、同じスタッフ・日付・時間帯の重複は不可）
- エラー画面（存在しない ID は 404、URL の形式が不正なら 400、それ以外の想定外エラーは 500）

時間帯は次の4種類です。

| 区分 | 時間 |
|---|---|
| 早番 | 09:00〜13:00 |
| 遅番 | 13:00〜18:00 |
| 夜番 | 18:00〜22:00 |
| 通し | 09:00〜18:00 |

## 使った技術

| 分類 | 内容 |
|---|---|
| 言語 | Java 21 |
| フレームワーク | Spring Boot 3.5.16（Spring MVC / Spring Data JPA / Spring Security / Bean Validation） |
| 画面 | Thymeleaf（thymeleaf-extras-springsecurity6） |
| DB | H2 Database（インメモリ） |
| ビルド | Maven |
| テスト | JUnit 5、AssertJ、Spring Boot Test（`@DataJpaTest`、MockMvc）、spring-security-test |
| CI | GitHub Actions（push と pull request でテストを自動実行） |

Spring Boot は 3 系で最後の OSS リリースである 3.5.16 を使っています。3.5 系の OSS サポートは終了しているため、実運用に使うなら 4.x への移行が必要です。

## 動かし方

必要なもの: JDK 21 と Maven 3.9 以上

```bash
git clone https://github.com/tyadr18/shift-board.git
cd shift-board
mvn spring-boot:run
```

起動したら、ブラウザで http://localhost:8080 を開きます。

### ログイン方法

環境変数を設定せずに起動すると、デモ用の初期値でログインできます。

| 項目 | デモ用の初期値 | 変更するときの環境変数 |
|---|---|---|
| ユーザー名 | `demo` | `APP_USERNAME` |
| パスワード | `demo-pass` | `APP_PASSWORD` |

デモ用の初期値で起動しているときだけ、ログイン画面にもこの値が表示されます。起動ログにも警告が出ます。初期値はデモ専用です。ほかの人がアクセスできる環境で動かすときは、必ず `APP_PASSWORD` に自分で決めたパスワードを設定してください。

パスワードを指定して起動する例:

```bash
# macOS / Linux
APP_USERNAME=manager APP_PASSWORD='任意の強いパスワード' mvn spring-boot:run
```

```powershell
# Windows（PowerShell）
$env:APP_USERNAME = "manager"
$env:APP_PASSWORD = "任意の強いパスワード"
mvn spring-boot:run
```

起動時には、架空のサンプルデータが4件入ります（日付は起動日の翌日以降）。空の状態で始めたいときは `APP_DEMO_DATA=false` を設定してください。DB はインメモリなので、アプリを止めるとデータは消えます。

### テストの実行

```bash
mvn test
```

## 工夫点

- **層ごとに役割を分けた**: 画面の入力値（`ShiftRequestForm`）と、サービスに渡す値（`ShiftRequestCommand`）を別のクラスにしています。形式チェック（必須・文字数）はフォームの Bean Validation で、業務ルール（過去日・重複）はサービス層で行います。
- **業務ルール違反は入力欄の横に表示する**: サービス層は `ShiftRuleViolationException` に「どの項目の問題か」を持たせて投げます。コントローラーはそれを `BindingResult` に積み直すので、通常の入力エラーと同じ見た目でメッセージが出ます。
- **重複は二重に防ぐ**: 保存前に同じ希望があるかを確認し、DB にも一意制約を張っています。同時に登録されて事前チェックをすり抜けた場合は、`GlobalExceptionHandler` が 409 のエラー画面を返します。
- **「今日」を差し替えられるようにした**: 過去日の判定には `Clock` Bean を使います。テストでは日付を固定するので、実行する日によって結果が変わりません。
- **検索条件を組み合わせられるようにした**: Spring Data JPA の `Specification` で条件を小さく分け、入力された条件だけを組み合わせます。スタッフ名の検索では `%` や `_` をエスケープし、ワイルドカードではなく文字として扱います。
- **パスワードをコードに書かない**: ログイン情報は環境変数から読み込みます。初期値のまま起動したときは、警告ログとログイン画面の表示で分かるようにしています。CSRF 対策は Spring Security の既定のまま有効にしています。
- **テストは2段構え**: サービス層は `@DataJpaTest` を使い、実際の H2 に対して検索・並び順・ルール違反を確認します。画面は MockMvc で、ログイン成功・失敗、ログアウト、未ログイン時のリダイレクト、CSRF、入力エラー、404/400 を確認します。

### ブラウザでの確認ポイント

自動テストと同じ内容を、ブラウザでは次の操作で確かめられます。

1. ログアウト後に `/shifts` を開くと、ログイン画面に戻る
2. 日付欄を空にしたり、備考を 201 文字以上にしたりすると、該当する欄にエラーが出る（入力した値は消えない）
3. 同じスタッフ・日付・時間帯で2回登録すると、2回目は時間帯欄にエラーが出る
4. 絞り込みで開始日を終了日より後にすると、エラーが出て一覧は 0 件になる
5. `/shifts/9999/edit` は 404、`/shifts/abc/edit` は 400 のエラー画面になる

## ディレクトリ構成

```
shift-board/
├── .github/workflows/test.yml        # GitHub Actions（mvn verify）
├── pom.xml
└── src
    ├── main
    │   ├── java/com/tyadr18/shiftboard
    │   │   ├── ShiftBoardApplication.java
    │   │   ├── config/               # セキュリティ、ログイン設定、Clock、サンプルデータ投入
    │   │   ├── domain/               # エンティティ（ShiftRequest）と時間帯（TimeSlot）
    │   │   ├── repository/           # Spring Data JPA リポジトリと検索条件（Specification）
    │   │   ├── service/              # 業務ロジック、入力値・検索条件、業務例外
    │   │   └── web/                  # コントローラー、フォーム、例外ハンドラー
    │   └── resources
    │       ├── application.properties
    │       ├── messages.properties   # 型変換エラーのメッセージ
    │       ├── static/css/style.css
    │       └── templates/            # login / shifts/list / shifts/form / error / fragments/layout
    └── test/java/com/tyadr18/shiftboard
        ├── service/ShiftRequestServiceTest.java    # サービス層（@DataJpaTest）
        └── web/
            ├── LoginFlowTest.java                  # ログイン・ログアウト（MockMvc）
            └── ShiftRequestControllerTest.java     # 画面・入力チェック・例外（MockMvc）
```

## ライセンス

MIT License
