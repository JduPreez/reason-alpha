(ns ra-datagrid.events
  (:require [medley.core :refer [dissoc-in]]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn get-next-sort-direction
  [c]
  (condp = c
    nil  :asc
    :asc :desc
    :desc :none
    :none :asc))

(defn extend-options-with-defaults
  [options]
  (let [merged (merge
                {:can-edit                    true
                 :can-create                  true
                 :can-delete                  true
                 :delete-are-you-sure-message "De gegevens kunnen niet meer worden teruggehaald."
                 :debug                       false}
                options)]
    merged))


(defn register-scroll-events
  [id]
  ;;whenever there 's a div re-datagrid-read-more-marker IN the viewport
  ;;dispatch the expand max-num-rows variable


  )

(rf/reg-event-db
 :datagrid/initialize
 (fn [db [_ opts fields]]
   (assert (:data-subscription opts)
           "No subscription for records. Please set a :data-subscription re-frame subscribe pattern on init-time.")
   (let [id (:grid-id opts)]
     (when (:progressive-loading opts)
       (register-scroll-events id))
     (assoc-in db [:datagrid/data id] {:options                 (extend-options-with-defaults opts)
                                       :fields                  fields
                                       :selected-records        #{}
                                       :are-you-sure-callback   nil
                                       :show-sure?              false
                                       :creating?               false
                                       :create-record           nil
                                       :expanded?               false
                                       :mass-select-check       false
                                       :rec-marked-for-deletion nil
                                       :header-filter-values    {}
                                       :edit-rows               {} ;; map of pk -> rec
                                       :sorting                 {:key       (:default-sort-key opts)
                                                                 :direction (when (:default-sort-key opts)
                                                                              (or (:default-sort-direction opts) :asc))}}))))

(rf/reg-event-db
 :datagrid/update-options
 (fn [db [_ grid-id o]]
   (assoc-in db [:datagrid/data grid-id :options] o)))


(rf/reg-event-db
 :datagrid/update-fields
 (fn [db [_ grid-id o]]
   (assoc-in db [:datagrid/data grid-id :fields] o)))



(rf/reg-event-fx
 :datagrid/sort-field
 (fn [{db :db} [_ grid-id field-name]]
   (let [nd             (get-next-sort-direction (get-in db [:datagrid/data grid-id :sorting :direction]))
         extra-dispatch (get-in db [:datagrid/data grid-id :options :sort-dispatch])
         extra-dispatch (when extra-dispatch
                          (vec (concat extra-dispatch [field-name nd])))]
     (js/console.log "Sorting" grid-id ", field:" field-name " in direction:" nd)
     (cond-> {:db
              (-> db
                  (assoc-in [:datagrid/data  grid-id :sorting :key] field-name)
                  (update-in [:datagrid/data  grid-id :sorting :direction] get-next-sort-direction))}
       extra-dispatch (assoc :dispatch extra-dispatch)))))

(rf/reg-event-fx
 :datagrid/toggle-mass-select
 (fn [{:keys [db]} [_ id all-records]]
   (let [id-field        (get-in db [:datagrid/data id :options :id-field])
         checked?        (get-in db [:datagrid/data id :mass-select-check])
         select-dispatch (get-in db [:datagrid/data id :options :select-dispatch])
         selected        (if checked? ;;now it won't be
                           #{}
                           (->>
                            all-records
                            (map id-field)
                            set))]
     (cond-> {:db db}
       select-dispatch (assoc :dispatch [select-dispatch selected all-records])
       :always         (update :db #(-> %
                                        (update-in [:datagrid/data id :mass-select-check] not)
                                        (assoc-in [:datagrid/data id :selected-records]
                                                  selected)))))))

(rf/reg-event-fx
 :datagrid/toggle-checkbox
 (fn [{:keys [db]} [_ grid-id record]]
   (let [id-field        (get-in db [:datagrid/data grid-id :options :id-field])
         select-dispatch (get-in db [:datagrid/data grid-id :options :select-dispatch])
         pk              (get record id-field)
         prev-selected   (get-in db [:datagrid/data grid-id :selected-records])
         next-selected   (if (some #{pk} prev-selected)
                           (disj prev-selected pk)
                           (conj prev-selected pk))]
     (cond-> {:db db}
       select-dispatch (assoc :dispatch [select-dispatch next-selected [record]])
       :always         (update-in [:db :datagrid/data grid-id :selected-records]
                                  (constantly next-selected))))))

(rf/reg-event-db
 :datagrid/create-new-record
 (fn [db [_ grid-id]]
   (-> db
       ;;put it under 'nil' key in edit-rows
       (assoc-in [:datagrid/data grid-id :edit-rows nil] {} #_(or defaults {}))
       (assoc-in [:datagrid/data grid-id :creating?] true))))

(rf/reg-event-db
 :datagrid/update-edited-record
 (fn [db [_ grid-id pk k v]]
   (cljs.pprint/pprint [grid-id pk k v])
   (if (not (nil? v))
     (assoc-in db [:datagrid/data grid-id :edit-rows pk k] v)
     (dissoc-in db [:datagrid/data grid-id :edit-rows pk k]))))

;;rec-with-only-grid-fields (if is-update?
 ;;                           (assoc (remove-keys-not-in-gridfields @edit-record fields)
  ;;                                 (:id-field options) (get @edit-record (:id-field options)))
   ;;                         (remove-keys-not-in-gridfields @edit-record fields))
;;rec-with-added-defaults   (if (:default-values options)
 ;;                           (merge (:default-values options) rec-with-only-grid-fields)
  ;;                          rec-with-only-grid-fields)

(rf/reg-event-fx
 :datagrid/save-edited-record
 (fn [{db :db} [_ id pk]]
   (if (nil? pk) ;; it's a create, redispatch
     {:db       db
      :dispatch [:datagrid/save-new-record id]}
     ;;else
     (let [fields          (get-in db [:datagrid/data id :fields])
           id-field        (get-in db [:datagrid/data id :options :id-field])
           defaults        (get-in db [:datagrid/data id :options :default-values])
           update-dispatch (get-in db [:datagrid/data id :options :update-dispatch])
           keys-from-grid  (map :name fields)
           r               (get-in db [:datagrid/data id :edit-rows pk])
           #_#_r           (as-> (get-in db [:datagrid/data id :edit-rows pk]) r'
                             (select-keys r' keys-from-grid)
                             (assoc r' id-field pk)
                             (merge defaults r'))
           update-dispatch (conj update-dispatch r)]
       {:db       (update-in db [:datagrid/data id :edit-rows] dissoc pk)
        :dispatch update-dispatch}))))


(rf/reg-event-fx
 :datagrid/save-new-record
 (fn [{db :db} [_ id]]
   (let [edited-record (get-in db [:datagrid/data  id :edit-rows nil])
         dispatch      (-> (get-in db [:datagrid/data id :options :create-dispatch])
                           (conj edited-record))]
     {:db       (-> db
                    (update-in [:datagrid/data id :edit-rows] dissoc nil)
                    (assoc-in  [:datagrid/data id :creating?] false))
      :dispatch dispatch})))

(rf/reg-event-db
 :datagrid/toggle-expand
 (fn [db [_ id]]
   (update-in db [:datagrid/data  id :expanded?] not)))

(rf/reg-event-fx
 :datagrid/start-edit
 (fn [{db :db} [_ id pk record]]
   (let [start-edit-dispatch (get-in db [:datagrid/data id :options :start-edit-dispatch])]
     (merge
      {:db (assoc-in db [:datagrid/data id :edit-rows pk] record)}
      (when start-edit-dispatch
        {:dispatch (conj start-edit-dispatch record)})))))

(rf/reg-event-fx
 :datagrid/reorder
 (fn [{db :db} [_ id direction record]]
   (assert (get-in db [:datagrid/data id :options :reorder-dispatch]) "There is no :reorder-dispatch set in the options!")
   (let [disp (-> db
                  (get-in [:datagrid/data id :options :reorder-dispatch])
                  (concat [direction record])
                  vec)]
     {:db       db
      :dispatch disp})))

(rf/reg-event-db
 :datagrid/show-are-you-sure-popup
 (fn [db [_ id show?]]
   (assoc-in db [:datagrid/data id :show-sure?] show?)))

(rf/reg-event-db
 :datagrid/update-history
 (fn [db [_ id]]
   (if id
     (as-> (get-in db [:datagrid/history]) h
       (or h '())
       (remove #(= id %) h)
       (conj h id)
       (assoc-in db [:datagrid/history] h))
     db)))

(rf/reg-event-fx
 :datagrid/delete-record-maybe
 (fn [{db :db} [_ id record]]
   (let [show? (get-in db [:datagrid/data id :options :delete-are-you-sure-message])]
     (cond-> {:db (-> db
                      (assoc-in [:datagrid/data id :rec-marked-for-deletion] record)
                      (assoc-in [:datagrid/data id :show-sure?] show?))}
       (not show?)
       (assoc :dispatch [:datagrid/delete-record id])))))

(rf/reg-event-fx
 :datagrid/delete-record
 (fn [{db :db} [_ id]]
   (let [delete-dispatch (get-in db [:datagrid/data id :options :delete-dispatch])
         record          (get-in db [:datagrid/data id :rec-marked-for-deletion])]
     (assert record)
     {:db       (-> db
                    (assoc-in [:datagrid/data id :show-sure?] false)
                    (assoc-in [:datagrid/data id :options :rec-marked-for-deletion] nil))
      :dispatch (conj delete-dispatch record)})))

(rf/reg-event-fx
 :datagrid/header-filter-value
 (fn [{db :db} [_ id k v blur?]]
   (let [extra-dispatch (get-in db [:datagrid/data id :options :header-filter-dispatch])
         extra-dispatch (when extra-dispatch
                          (vec (concat extra-dispatch [id k v blur?])))]
     (cond->
         {:db (assoc-in db [:datagrid/data id :header-filter-values k] v)}
       extra-dispatch (assoc :dispatch extra-dispatch)))))

(rf/reg-event-db
 :datagrid/cancel-editing
 (fn [db [_]]
   (->> db
        :datagrid/data
        (map (fn [[gid d]]
               [gid (dissoc d :edit-rows :creating?)]))
        (into {})
        (assoc db :datagrid/data))))
