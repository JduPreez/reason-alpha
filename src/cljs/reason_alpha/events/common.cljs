(ns reason-alpha.events.common
  (:require [re-frame.core :as rf]
            [reason-alpha.data :as data]
            [reason-alpha.utils :as utils]))

(defn handle-delete!-result-fn [entities-name model-type]
  (fn [{:keys [db]} [evt {type                                 :type
                          {:keys [deleted referenced-not-del]} :result
                          :as                                  r}]]
    (utils/log evt r)

    (if (= type :success)
      (let [{del-db   :db
             del-disp :dispatch} (when deleted
                                   (data/delete-local! {:db         db
                                                        :model-type model-type
                                                        :data       deleted}))
            not-del-disp         (when (seq referenced-not-del)
                                   [:alert/send (merge r {:title       (str "Unable to delete some " entities-name)
                                                          :description (str "Some " entities-name " are still referenced, "
                                                                            "and couldn't be deleted")
                                                          :type        :warn})])]
        {:db         (or del-db db)
         :dispatch-n (cond-> []
                       deleted            (conj del-disp)
                       referenced-not-del (conj not-del-disp))})
      {:db       db
       :dispatch [:alert/send r]})))
