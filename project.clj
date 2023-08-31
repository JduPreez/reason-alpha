(defproject reason-alpha "0.1.0-SNAPSHOT"
  :description      "FIXME: write description"
  :url              "http://example.com/FIXME"
  :min-lein-version "2.9.1"
  :license          {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
                     :url  "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [[buddy/buddy-core "1.10.413"]
                 [buddy/buddy-sign "3.4.333"]
                 [cc.qbits/spandex "0.7.6"]
                 [cheshire "5.8.1"]
                 [cljs-ajax "0.8.0"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [com.cognitect/transit-clj  "1.0.324"]
                 [com.cognitect/transit-cljs "0.8.264"]
                 [com.github.f4b6a3/uuid-creator "1.4.2"]
                 [com.github.igrishaev/pact "0.1.1"]
                 [com.google.javascript/closure-compiler-unshaded "v20210505" :scope "provided"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [com.outpace/config "0.13.5"]
                 [compojure "1.6.2" :exclusions [[medley]]]
                 [com.taoensso/sente "1.16.2"]
                 [day8.re-frame/http-fx "0.2.4"]
                 [dev.weavejester/medley "1.7.0"]
                 [http-kit "2.5.3"]
                 [integrant "0.8.0"]
                 [io.xapix/axel-f "2.0.11"]
                 [cljsjs/jquery "3.2.1-0"]
                 [com.xtdb/xtdb-core "1.20.0"]
                 [com.xtdb/xtdb-rocksdb "1.20.0"]
                 [markdown-clj "1.10.0"]
                 [me.raynes/fs "1.4.6"]
                 [metosin/malli "0.6.0-SNAPSHOT"]
                 [metosin/muuntaja "0.6.4"]
                 [metosin/reitit "0.3.10"]
                 [metosin/ring-http-response "0.9.1"]
                 [migratus "1.2.6"]
                 [nrepl "0.6.0"]
                 [org.babashka/sci "0.3.5"]
                 [org.clojure/clojure "1.10.3"]
                 [org.clojure/clojurescript "1.10.879" :scope "provided"]
                 [org.clojure/core.async "0.4.500"]
                 [org.clojure/core.cache "1.0.217"]
                 [org.clojure/data.codec "0.1.1"]
                 [org.clojure/google-closure-library "0.0-20201211-3e6c510d" :scope "provided"]
                 [org.clojure/tools.cli "1.0.206"]
                 [org.clojure/tools.logging "1.1.0"]
                 [org.clojure/tools.reader "1.3.6"]
                 [org.slf4j/slf4j-log4j12 "1.7.29"]
                 [pogonos "0.2.1"]
                 [prone "2019-07-08"]
                 [prismatic/schema "1.1.6"]
                 [re-frame "1.2.0"]
                 [reagent "1.1.0"]
                 [re-com "2.13.2"]
                 [ring "1.9.1"]
                 [ring-cors "0.1.13"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-anti-forgery "1.3.0"]
                 [ring/ring-defaults "0.3.2"]
                 [ring-ttl-session "0.3.1"]
                 [selmer "1.12.17"]
                 [thheller/shadow-cljs "2.15.2" :scope "provided"]
                 [tick "0.5.0-RC5"]
                 [traversy "0.5.0"]]

  :main ^:skip-aot reason-alpha.core

  :jvm-opts ["-Xmx2g"]

  :plugins [[migratus-lein "0.7.2"]]

  :source-paths ["src/cljc" "src/clj" "src/cljs"]
  :test-paths ["test/clj"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s"

  :clean-targets ^{:protect false}

  [:target-path "target/cljsbuild"]

  :profiles {:uberjar {:aot :all}

             :dev          [:project/dev]
             :production   [:project/prod]
             :test         [:project/dev :project/test]
             :project/dev  {:dependencies   [[binaryage/devtools "0.9.10"]
                                             [cider/piggieback "0.4.2"]
                                             [circleci/circleci.test "0.4.2"]
                                             [day8.re-frame/tracing "0.6.2"]
                                             [day8.re-frame/re-frame-10x "1.1.13"]
                                             [javax.servlet/servlet-api "2.5"]
                                             [re-frisk "0.5.4.1"]
                                             [ring/ring-devel "1.8.0"]
                                             [ring/ring-mock "0.4.0"]]
                            :plugins        [[jonase/eastwood "0.3.5"]]
                            :source-paths   ["env/dev/clj" "env/dev/cljs" "test/cljs"]
                            :resource-paths ["env/dev/resources"]
                            :repl-options   {:init-ns user}
                            :jvm-opts       ["-XX:-OmitStackTraceInFastThrow"]}
             :project/prod {:env          {:production true}
                            :source-paths ["env/prod/clj" "env/prod/cljs"]}
             :project/test {:jvm-opts       ["-Dconf=test-config.edn"]
                            :resource-paths ["env/test/resources"]}}

  :aliases {"test"   ["run" "-m" "circleci.test/dir" :project/test-paths]
            "tests"  ["run" "-m" "circleci.test"]
            "retest" ["run" "-m" "circleci.test.retest"]})
