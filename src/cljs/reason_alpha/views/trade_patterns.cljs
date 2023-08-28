(ns reason-alpha.views.trade-patterns
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [reason-alpha.utils :as utils]
            [reason-alpha.views.datagrid :as datagrid]))

(def options
  {:grid-id           ::view
   :title             "Trade Patterns"
   :data-subscription [:trade-pattern/list]
   :id-field          :trade-pattern-creation-id
   :create-dispatch   [:trade-pattern.command/create]
   :update-dispatch   [:trade-pattern.command/update]})

(defn view []
  (fn []
    (let [*schema (rf/subscribe [:model :model/trade-pattern-dto])
          fields  (datagrid/model->fields
                   @*schema
                   {:fields-opts
                    {:trade-pattern-parent-id
                     {:indent-group {:group-path   [:trade-pattern-id]
                                     :display-name :trade-pattern-name}}}})]
      [datagrid/view fields options])))

