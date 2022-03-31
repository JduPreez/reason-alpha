(ns ra-datagrid.config
  (:require [cljs-time.format :as fmt]))

(def debug?
  ^boolean goog.DEBUG)

(def ^:const date-format "dd-MM-yyyy")
(def time-formatter (fmt/formatter "dd-MM-yyyy HH:mm"))
(def date-formatter (fmt/formatter date-format))
