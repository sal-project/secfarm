(defproject secfarm "0.4.8"
  :description "社内等におけるeラーニング支援ツール"
  :url "https://github.com/sal-project/secfarm"
  :license {:name "MIT"
            :url "https://opensource.org/license/mit/"}
  :dependencies [[org.clojure/clojure "1.11.2"]
                 [ring "1.12.1"]
                 [metosin/reitit "0.6.0"]
                 [metosin/spec-tools "0.10.6"]
                 [metosin/muuntaja "0.6.10"]
                 [selmer "1.12.59"]
                 [buddy/buddy-core "1.11.423"]
                 [buddy/buddy-auth "3.0.323"]
                 [buddy/buddy-hashers "2.0.167"]
                 [com.github.seancorfield/next.jdbc "1.3.925"]
                 [org.postgresql/postgresql "42.7.3"]
                 [hikari-cp "3.0.1"]
                 [com.github.seancorfield/honeysql "2.6.1126"]
                 [dev.weavejester/ragtime "0.9.4"]
                 [org.clojure/tools.logging "1.3.0"]
                 [org.slf4j/slf4j-api "2.0.7"]
                 [ch.qos.logback/logback-classic "1.4.8"]
                 [clojure.java-time "1.2.0"]
                 [com.stuartsierra/component "1.1.0"]
                 [markdown-clj "1.12.1"]]
  :repl-options {:init-ns secfarm.core}
  :main ^:skip-aot secfarm.core
  :target-path "target/%s"
  :resource-paths ["resource"]
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :production {:resource-paths ["resources"]}
             :dev {:source-paths ["dev/src"]
                   :resource-paths ["dev/resources" "resources"]
                   :dependencies [[com.stuartsierra/component.repl "0.2.0"]]}
             :test {:dependencies []}
             :repl {:repl-options {:init-ns user}}})
