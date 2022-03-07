(ns reason-alpha.views.instruments
  (:require [reason-alpha.views.datagrid :as datagrid]
            [re-frame.core :as rf]))

(def options
  {:grid-id           ::view
   :title             "Instruments"
   :data-subscription [:instrument/list]
   :id-field          :instrument-creation-id
   :can-sort          true
   :create-dispatch   [:instrument.command/create]
   :update-dispatch   [:instrument.command/save!]
   :default-values    {}})

(defn view []
  (let [*schema (rf/subscribe [:model :model/instrument-dao])
        flds    (datagrid/model->fields @*schema)]
    [datagrid/view flds options]))
