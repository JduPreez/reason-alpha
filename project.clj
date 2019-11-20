(defproject reason-alpha "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [[cheshire "5.8.1"]
                 [cljs-ajax "0.8.0"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [com.cognitect/transit-clj "0.8.319"]
                 [com.github.f4b6a3/uuid-creator "1.4.2"]
                 [com.google.javascript/closure-compiler-unshaded "v20190618" :scope "provided"]
                 [day8.re-frame/http-fx "0.1.6"]
                 [luminus-transit "0.1.1"]
                 [markdown-clj "1.10.0"]
                 [metosin/muuntaja "0.6.4"]
                 [metosin/reitit "0.3.10"]
                 [migratus "1.2.6"]
                 [nrepl "0.6.0"]
                 [org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.520" :scope "provided"]
                 [org.clojure/core.async "0.4.500"]
                 [org.clojure/tools.cli "0.4.2"]
                 [org.clojure/tools.logging "0.5.0"]                 
                 [org.clojure/tools.reader "1.3.2"]
                 [org.apache.ignite/ignite-core "2.7.5"]
                 [org.apache.ignite/ignite-indexing "2.7.5"]
                 [org.apache.ignite/ignite-spring "2.7.5"]
                 [org.apache.ignite/ignite-spring-data "2.7.5"]
                 [org.clojure/java.jdbc "0.7.10"]
                 [org.slf4j/slf4j-log4j12 "1.7.29"]
                 [re-frame "0.10.9"]
                 [reagent "0.8.1"]
                 [ring "1.8.0"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-defaults "0.3.2"]
                 [ring-ttl-session "0.3.1"]
                 [selmer "1.12.17"]
                 [thheller/shadow-cljs "2.8.71" :scope "provided"]]

  :main ^:skip-aot reason-alpha.core

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
   :builds {:app  {:target     :browser
                   :output-dir "target/cljsbuild/public/js"
                   :asset-path "/js"
                   :modules    {:app {:entries [reason-alpha.app]}}
                   :devtools   {:watch-dir "resources/public"
                                :preloads  [re-frisk.preload]}
                   :dev        {:closure-defines {"re_frame.trace.trace_enabled_QMARK_" true}}}
            :test {:target    :node-test
                   :output-to "target/test/test.js"
                   :autorun   true}}}
  :npm-deps [[shadow-cljs "2.8.69"]
             [create-react-class "15.6.3"]
             [react "16.8.6"]
             [react-dom "16.8.6"]
             ["@ag-grid-enterprise/all-modules" "^22.0.0"]
             ["@ag-grid-community/react" "^22.0.0"]]

  :profiles {:uberjar {:aot :all}
             :dev     {:dependencies   [[binaryage/devtools "0.9.10"]
                                        [cider/piggieback "0.4.2"]                                        
                                        [javax.servlet/servlet-api "2.5"]
                                        [midje "1.9.9"]
                                        [prone "2019-07-08"]
                                        [re-frisk "0.5.4.1"]
                                        [ring/ring-devel "1.8.0"]
                                        [ring/ring-mock "0.4.0"]]
                       :plugins        [[jonase/eastwood "0.3.5"]]
                       :source-paths   ["env/dev/clj" "env/dev/cljs" "test/cljs"]
                       :resource-paths ["env/dev/resources"]
                       :repl-options   {:init-ns user}}})
