secfarm
=======

# 概要

SALプロジェクトの模擬環境向けの学習支援ツールです。
本ツールはWebアプリケーションとして動作します。

# セットアップ

## インストール

セットアップ方法は[こちらを参照](https://github.com/sal-project/sal-setup-document)してください。

## 設定

使用する前に設定ファイルを適切に記述する必要があります。

### 設定ファイルを開く

ツールの設定はresources/config.ednに記述します。
ファイルは[edn形式](https://clojuredocs.org/clojure.edn)で記載します。

    $ cd secfarm
    $ emacs resources/config.edn
    
### 設定を記載する

設定ファイルにはサーバー設定とDB接続設定を記載します。
下記に設定ファイル例を記載します。

    {:server {:port 3000 ;; 1. ポート番号
              :lecdata ["/srv/dev/secfarm-prod/lecdata"]} ;; 2. 研修資料ファイルの場所
     :db {:datasource-option {:auto-commit        true
                              :read-only          false
                              :connection-timeout 30000
                              :validation-timeout 5000
                              :idle-timeout       600000
                              :max-lifetime       1800000
                              :minimum-idle       10
                              :maximum-pool-size  10
                              :pool-name          "db-pool"
                              :adapter            "postgresql"   ;; 本ツールはpostgresqlにのみ対応しています
                              :username           "devel"        ;; 3. データベース接続ユーザー名
                              :password           "devel"        ;; 4. データベース接続ユーザーのパスワード
                              :database-name      "secfarmdevel" ;; 5. データベース名
                              :server-name        "localhost"    ;; 6. データベースサーバー
                              :port-number        5432           ;; 7. データベースサーバーポート
                              :register-mbeans    false}}}
                              
| 設定項目 | 設定内容                                                                                                                                                                                                |
|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1        | 本アプリケーションがListenするポート番号を記載します                                                                                                                                                    |
| 2        | 研修資料が配置されたディレクトリを指定します。例えば、[sal-lecture-document](https://github.com/sal-project/sal-lecture-document)を任意のディレクトリに配置し、配置したディレクトリのパスを記載します。 |
| 3        | データベースサーバーへ接続するユーザー名を指定します                                                                                                                                                    |
| 4        | データベースサーバーへ接続するユーザーに対応したパスワードを記載します                                                                                                                                  |
| 5        | データベースサーバー上で使用するデータベース名を記載します                                                                                                                                              |
| 6        | データベースサーバーのIPアドレスやドメイン名を記載します                                                                                                                                                |
| 7        | データーベースサーバーのポート番号を記載します                                                                                                                                                          |

## 起動

プロジェクト配下にて、下記のコマンドを実行することでproductionモードで起動することができます。

    $ lein with-profile production run

# ライセンス

LICENSEファイルを参照してください。
