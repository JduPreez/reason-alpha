(ns ra-datagrid.utils)

(defn maybe-parse-number [nr]
  (cond
    (re-find #"^-?\d+\.\d+$" nr) (js/parseFloat nr)
    (re-find #"^-?\d+$" nr)      (js/parseInt nr)
    :else                        nr))
