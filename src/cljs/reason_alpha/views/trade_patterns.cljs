(ns reason-alpha.views.trade-patterns
  (:require ["@ag-grid-community/react" :as ag-grd-react]
            ["@ag-grid-enterprise/all-modules" :as ag-grd]
            [re-frame.core :as rf]
            [reason-alpha.views.data-grid :as data-grid]))

(def model :trade-patterns)

;; TODO 2: Hookup to toolbar menu's Add button event & add a new row to
;;         the data grid similar to
;;         https://www.ag-grid.com/javascript-grid-data-update/?framework=javascript#example-updating-with-transaction
(defn view []
  (fn []
    (data-grid/view {:fn-save   #(rf/dispatch [:save :trade-patterns %])
                     :fn-get-id (fn [{:keys [trade-pattern/id
                                             trade-pattern/creation-id]
                                      :as   data}]
                                  (cljs.pprint/pprint (or id creation-id))
                                  (or id creation-id))}
                    @(rf/subscribe [:trade-patterns])
                    {:trade-pattern/name        {:header    "Trade Pattern"
                                                 :flex      1
                                                 :min-width 200
                                                 :max-width 230
                                                 :editable? true}
                     :trade-pattern/parent-id   {:header    "Parent"
                                                 :flex      1
                                                 :min-width 200
                                                 :max-width 250
                                                 :select    {:lookup-key :trade-pattern/id
                                                             :*options   (rf/subscribe [:trade-pattern-options])}
                                                 :editable? true}
                     :trade-pattern/description {:header    "Description"
                                                 :flex      2
                                                 :editable? true}}
                    :trade-pattern/ancestors-path
                    #(rf/dispatch [:select %]))))

