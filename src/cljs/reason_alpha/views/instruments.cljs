(ns reason-alpha.views.instruments
  (:require [reason-alpha.views.datagrid :as datagrid]
            [re-frame.core :as rf]))

(def options
  {:grid-id           ::view
   :title             "Instruments"
   :data-subscription [:instrument/list]
   :id-field          :instrument-creation-id
   :create-dispatch   [:instrument.command/create]
   :update-dispatch   [:instrument.command/update]
   :default-values    {:instrument-type [:share ""]}})

(defn view []
  (let [*schema            (rf/subscribe [:model :model/instrument-dto])
        *instr-type-titles (rf/subscribe [:instrument/type-titles])
        fields             (datagrid/model->fields
                            @*schema
                            {:fields-opts
                             {:instrument-type
                              {:enum-titles @*instr-type-titles}}})]
    [datagrid/view fields options]))
