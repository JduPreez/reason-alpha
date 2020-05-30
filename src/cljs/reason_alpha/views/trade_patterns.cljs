(ns reason-alpha.views.trade-patterns
  (:require ["@ag-grid-community/react" :as ag-grd-react]
            ["@ag-grid-enterprise/all-modules" :as ag-grd]           
            [re-frame.core :as rf]
            [reason-alpha.views.data-grid :as data-grid]))
;; TODO 1: Convert entire reason-alpha.html body to hiccup component
;; TODO 2: Hookup to toolbar menu's Add button event & add a new row to
;;         the data grid similar to
;;         https://www.ag-grid.com/javascript-grid-data-update/?framework=javascript#example-updating-with-transaction
;; TODO 2: Group data by parent
;;         |> https://stackoverflow.com/questions/42605168/ag-grid-try-to-make-tree-demo-work-using-own-data

(defn view []
  (data-grid/view @(rf/subscribe [:trade-patterns]) 
                  {:trade-pattern/name        {:header    "Trade Pattern"
                                               :flex      1
                                               :min-width 200
                                               :max-width 230
                                               :editable  true}
                   :trade-pattern/parent-id   {:header    "Parent"
                                               :flex      1
                                               :min-width 200
                                               :max-width 250}
                   :trade-pattern/description {:header   "Description"
                                               :flex     2
                                               :editable true}}))
 
