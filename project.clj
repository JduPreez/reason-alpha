(defproject reason-alpha "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [ring "1.7.1"]
                 [org.apache.ignite/ignite-core "2.7.5"]
                 [org.apache.ignite/ignite-indexing "2.7.5"]
                 [org.apache.ignite/ignite-spring "2.7.5"]
                 [org.apache.ignite/ignite-spring-data "2.7.5"]
                 [migratus "1.2.6"]
                 [org.clojure/java.jdbc "0.7.10"]] #_[org.slf4j/slf4j-log4j12 "1.7.28"]

  :main ^:skip-aot reason-alpha.core

  :source-paths ["src/clj"]
  :test-paths ["test/clj"]
  :resource-paths ["resources"]
  :target-path "target/%s"

  :profiles {:uberjar {:aot :all}
             :dev     {:dependencies [[javax.servlet/servlet-api "2.5"]]}})
