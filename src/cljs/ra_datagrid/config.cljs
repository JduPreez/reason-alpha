(ns ra-datagrid.config
  (:require [cljs-time.format :as fmt]))

(def debug?
  ^boolean goog.DEBUG)

(def ^:const date-format "yyyy-MM-dd")
(def ^:const date-time-format (str date-format " HH:mm"))
(def time-formatter (fmt/formatter date-time-format))
(def date-formatter (fmt/formatter date-format))
