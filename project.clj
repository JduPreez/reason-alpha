(defproject reason-alpha "0.1.0-SNAPSHOT"
  :description      "FIXME: write description"
  :url              "http://example.com/FIXME"
  :min-lein-version "2.9.1"
  :license          {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
                     :url  "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [[cc.qbits/spandex "0.7.6"]
                 [cheshire "5.8.1"]
                 [cljs-ajax "0.8.0"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [com.cognitect/transit-clj "0.8.319"]
                 [com.github.f4b6a3/uuid-creator "1.4.2"]
                 [com.google.javascript/closure-compiler-unshaded "v20190618" :scope "provided"]
                 [day8.re-frame/http-fx "0.1.6"]
                 [functionalbytes/mount-lite "2.1.1"]
                 [juxt/crux-core "21.04-1.16.0-beta"]
                 [juxt/crux-rocksdb "21.04-1.16.0-beta"]
                 [luminus-transit "0.1.2"]
                 [markdown-clj "1.10.0"]
                 [medley "1.3.0"]
                 [metosin/muuntaja "0.6.4"]
                 [metosin/reitit "0.3.10"]
                 [metosin/ring-http-response "0.9.1"]
                 [migratus "1.2.6"]
                 [nrepl "0.6.0"]
                 [org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.520" :scope "provided"]
                 [org.clojure/core.async "0.4.500"]
                 [org.clojure/google-closure-library "0.0-20190213-2033d5d9" :scope "provided"]
                 [org.clojure/tools.cli "0.4.2"]
                 [org.clojure/tools.logging "0.5.0"]
                 [org.clojure/tools.reader "1.3.2"]
                 [org.apache.ignite/ignite-core "2.7.5"]
                 [org.apache.ignite/ignite-indexing "2.7.5"]
                 [org.apache.ignite/ignite-spring "2.7.5"]
                 [org.apache.ignite/ignite-spring-data "2.7.5"]
                 [org.clojure/java.jdbc "0.7.10"]
                 [org.slf4j/slf4j-log4j12 "1.7.29"]
                 [prone "2019-07-08"]
                 [re-frame "0.10.9"]
                 [reagent "0.8.1"]
                 [ring "1.8.0"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-defaults "0.3.2"]
                 [ring-ttl-session "0.3.1"]
                 [selmer "1.12.17"]
                 [thheller/shadow-cljs "2.8.71" :scope "provided"]
                 [traversy "0.5.0"]]

  :main ^:skip-aot reason-alpha.core

  :jvm-opts ["-Xmx2g"]

  :plugins [[lein-shadow "0.1.7"]
            [migratus-lein "0.7.2"]]

  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :test-paths ["test/clj"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s"

  :clean-targets ^{:protect false}

  [:target-path "target/cljsbuild"]

  :shadow-cljs
  {:nrepl  {:port 7002}
   :builds {:app  {:target                                                                                                                                                                                                                                                                                                                                                                                                     :browser
                   :output-dir                                                                                                                                                                                                                                                                                                                                                                                                 "resources/public/js/compiled"
                   :asset-path                                                                                                                                                                                                                                                                                                                                                                                                 "/js/compiled"
                   :modules                                                                                                                                                                                                                                                                                                                                                                                                    {:app {:entries [reason-alpha.core]}}
                   :devtools                                                                                                                                                                                                                                                                                                                                                                                                   {:http-root "resources/public"
                                                                                                                                                                                                                                                                                                                                                                                                                                :preloads  [devtools.preload]} ; re-frisk.preload
                   :dev                                                                                                                                                                                                                                                                                                                                                                                                        {:compiler-options {:clojure-defines {re-frame.trace/trace-enabled?        true
                                                                                                                                                                                                                                                                                                                                                                                                                                                                     day8.re-frame.tracing/trace-enabled? true}}}}
            :test {:target    :node-test
                   :output-to "target/test/tests.js"
                   :autorun   true}}}

  :npm-deps [["@ag-grid-enterprise/all-modules" "^22.0.0"]
             ["@ag-grid-community/react" "^22.0.0"]
             [create-react-class "15.6.3"]
             [react "16.8.6"]
             [react-dom "16.8.6"]
             [shadow-cljs "2.8.69"]]

  :profiles {:uberjar {:aot :all}

             :dev          [:project/dev]
             :production   [:project/prod]
             :test         [:project/dev :project/test]
             :project/dev  {:dependencies   [[binaryage/devtools "0.9.10"]
                                             [cider/piggieback "0.4.2"]
                                             [circleci/circleci.test "0.4.2"]
                                             [day8.re-frame/tracing "0.5.1"]
                                             [javax.servlet/servlet-api "2.5"]
                                             [re-frisk "0.5.4.1"]
                                             [ring/ring-devel "1.8.0"]
                                             [ring/ring-mock "0.4.0"]]
                            :plugins        [[jonase/eastwood "0.3.5"]]
                            :source-paths   ["env/dev/clj" "env/dev/cljs" "test/cljs"]
                            :resource-paths ["env/dev/resources"]
                            :repl-options   {:init-ns user}}
             :project/prod {:env          {:production true}
                            :source-paths ["env/prod/clj" "env/prod/cljs"]}
             :project/test {:jvm-opts       ["-Dconf=test-config.edn"]
                            :resource-paths ["env/test/resources"]}}

  :aliases {"test"   ["run" "-m" "circleci.test/dir" :project/test-paths]
            "tests"  ["run" "-m" "circleci.test"]
            "retest" ["run" "-m" "circleci.test.retest"]})
