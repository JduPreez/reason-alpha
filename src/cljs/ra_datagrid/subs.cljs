(ns ra-datagrid.subs
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [cljs-time.coerce :as coerce]
            [cljs-time.format :as fmt]
            [ra-datagrid.config :as conf]))

(defn sensible-sort
  [k r]
  (let [v (get r k)]
    (if (string? v)
      (clojure.string/lower-case v)
      #_else (str v))))

(defn sort-records
  "Assumes a `:<k>-formatted` key exists in each record for k"
  [records fields k direction]
  (if (or (nil? k) (= :none direction))
    ;no sorting to be done, return records untouched.
    records
    (let [dir-fn  (if (= :asc direction) identity reverse)
          fmt-key (keyword (str (name k) "-formatted"))
          field   (first (filter #(= (:name %) k) fields))
          sort-fn (if (:sort-value-fn field)
                    #((:sort-value-fn field) (or (get % k) (get % fmt-key)) %)
                    (partial sensible-sort fmt-key))
          do-sort #(->> %
                        (sort-by sort-fn)
                        dir-fn)]
      (->> records
           do-sort
           (map #(if (contains? % :datagrid/children)
                   (assoc % :datagrid/children
                          (do-sort (:datagrid/children %)))
                    %))))))

(rf/reg-sub
 :datagrid/all
 (fn [db [_ id]]
   (get-in db [:datagrid/data id])))

(rf/reg-sub
 :datagrid/initialized?
 (fn [db [_ id]]
   (let [options (get-in db [:datagrid/data id :options])]
     (and
      (not (nil? options))
      (not (empty? options))))))

(rf/reg-sub
 :datagrid/data-subscription
 (fn [db [_ id]]
   (get-in db [:datagrid/data id :options :data-subscription])))

(rf/reg-sub
 :datagrid/records
 (fn [[_ subscription-path] _]
   (assert subscription-path "make sure subscription-path is set")
   (rf/subscribe subscription-path))
 (fn [records _]
   (js/console.log (count records) "records were found")
   records))

(rf/reg-sub
 :datagrid/fields
 (fn [db [_ id]]
   (get-in db [:datagrid/data id :fields])))

(rf/reg-sub
 :datagrid/options
 (fn [db [_ id]]
   (get-in db [:datagrid/data id :options])))

(rf/reg-sub
 :datagrid/title
 (fn [[_ id]]
   (rf/subscribe [:datagrid/options id]))
 (fn [{:keys [title]} _]
   title))


(rf/reg-sub
 :datagrid/sorting
 (fn [db [_ id]]
   (get-in db [:datagrid/data id :sorting])))

(defn is-match?
  [s q]
  (let [s (if (string? s) s (str s))]
    (cond
      (nil? q) true
      (empty? q) true

      (and
       (nil? s)
       (not (nil? q))) false

      :otherwise
      (clojure.string/includes?
       (clojure.string/lower-case s)
       (clojure.string/lower-case q)))))

(defn field-matches?
  [record fields acc [field query]]
  (let [v (get record (-> field name (str "-formatted") keyword))
        field (->> fields
                   (filter (comp #{field} :name))
                   first)]
    (and acc
         (if (:custom-filter-fn field)
           ((:custom-filter-fn field) v query record)
           (is-match? v query)))))

(defn record-matches-filters?
  [filters fields r]
  (reduce (partial field-matches? r fields)
          true filters))

(defn filter-by-header-filters
  [records filters fields]
  (filter (partial record-matches-filters? filters fields)
          records))

(defmulti default-formatter (fn [{t :type :as field}]
                              (or t :default)))

(defmethod default-formatter :default
  [_v _r]
  identity)

(defmethod default-formatter :yesno
  [_]
  (fn [v r]
    (if v "yes" "no")))

(defmethod default-formatter :date
  [_]
  (fn [v r]
    (if v
      (let [v (coerce/from-date v)]
        (fmt/unparse conf/date-formatter v))
      "")))

(defmethod default-formatter :date-time
  [_]
  (fn [v r]
    (if v
      (let [v (coerce/from-date v)]
        (fmt/unparse conf/time-formatter v))
      "")))

(defn apply-formatters
  "Applies formatters under <keyname>-formatted key"
  [fields record]
  (reduce
   (fn [rec
        {fmt :formatter
         k   :name
         :as field}]
     ;;add formatted value
     (let [fmt (or fmt (default-formatter field))]
       (assoc rec (keyword (str (name k) "-formatted"))
              (fmt (get record k) record))))
   record
   fields))

(rf/reg-sub
 ;;formats records according to the formatter
 :datagrid/formatted-records
 (fn [[_ id data-sub] _]
   [(rf/subscribe [:datagrid/fields id])
    (rf/subscribe [:datagrid/records data-sub])])
 (fn [[fields records]]
   (map (partial apply-formatters fields) records)))

(defn group-records [{:keys [records group-path member-key id-field]}]
  (if (seq group-path)
    (->> records
         (filter #(nil? (get % member-key)))
         (map #(assoc %
                      :datagrid/children
                      (filter
                       (fn [r]
                         (and (get-in % group-path)
                              (get r member-key)
                              (= (get-in % group-path) (get r member-key))))
                       records))))
    ;; else
    records))

(rf/reg-sub
 :datagrid/sorted-records
 (fn [[_ id data-sub] _]
  [(rf/subscribe [:datagrid/options id])
    (rf/subscribe [:datagrid/formatted-records id data-sub])
    (rf/subscribe [:datagrid/expanded? id])
    (rf/subscribe [:datagrid/sorting id])
    (rf/subscribe [:datagrid/fields id])
    (rf/subscribe [:datagrid/header-filter-values id])])
 (fn [[{{:keys [group-key member-key]} :group-by
        :keys                          [id-field show-max-num-rows]
        :as                            options} formatted-records expanded? sorting fields filters] _]
   (let [group-by (some (fn [{:keys [type] :as f}]
                          (when (= type :indent-group)
                            f))
                        fields)
         rs       (group-records {:records    formatted-records
                                  :group-path (-> group-by :indent-group :group-path)
                                  :member-key (:name group-by)
                                  :id-field   id-field})
         rs       (if (and (:key sorting)
                           (:direction sorting))
                    (sort-records rs fields (:key sorting) (:direction sorting))
                    rs)
         ;; Flatten records again
         rs       (->> (mapcat #(into [%] (:datagrid/children %)) rs)
                       (map #(dissoc % :datagrid/children)))
         rs       (if (:header-filters options)
                    (filter-by-header-filters rs filters fields)
                    rs)]
     (if (and show-max-num-rows (not expanded?))
       (take show-max-num-rows rs)
       rs))))

(rf/reg-sub
 :datagrid/mass-select-checked?
 (fn [db [_ id]]
   (get-in db [:datagrid/data id :mass-select-check])))

(rf/reg-sub
 :datagrid/primary-key
 (fn [db [_ id]]
   (get-in db [:datagrid/data id :options :id-field])))

(rf/reg-sub
 :datagrid/creating?
 (fn [db [_ id]]
   (get-in db [:datagrid/data id :creating?])))

(rf/reg-sub
 :datagrid/editing?
 (fn [[_ id] _]
   (rf/subscribe [:datagrid/edit-rows id]))
 (fn [edit-rows]
   (not (empty? edit-rows))))

(rf/reg-sub
 :datagrid/editing-record?
 (fn [[_ grid-id _] _]
   [(rf/subscribe [:datagrid/options grid-id])
    (rf/subscribe [:datagrid/edit-rows grid-id])])
 (fn [[options edit-rows] [_ grid-id record]]
   (let [pk     (:id-field options)
         rec-id (get record pk)]
     (get edit-rows rec-id))))

(rf/reg-sub
 :datagrid/edited-record-by-pk
 (fn [db [_ grid-id pk]]
   (get-in db [:datagrid/data grid-id :edit-rows pk])))

(rf/reg-sub
 :datagrid/edited-record-by-pk-with-validation
 (fn [db [_ grid-id pk]]
   (let [{:keys [validator
                 default-values]} @(rf/subscribe [:datagrid/options grid-id])
         defaults                 (when (and default-values (nil? pk))
                                    @(rf/subscribe default-values))
         r                        (merge defaults
                                         @(rf/subscribe [:datagrid/edited-record-by-pk
                                                         grid-id pk]))
         vres                     (when validator
                                    (validator r))]
     #_(cljs.pprint/pprint {::->>>-VALIDATED-RES {:R r
                                                  :V vres}})
     (cond-> {:result r}
       vres (assoc :validation vres)))))

(rf/reg-sub
 :datagrid/edited-record-valid?
 (fn [[_ grid-id pk]]
   (rf/subscribe [:datagrid/edited-record-by-pk-with-validation grid-id pk]))
 (fn [{v :validation} [_ _ _]]
   (empty? v)))

(rf/reg-sub
 :datagrid/selected-record-pks-internal
 (fn [db [_ id]]
   (get-in db [:datagrid/data id :selected-records])))

(rf/reg-sub
 :datagrid/selected-record-pks
 (fn [[_ id data-sub]]
   (assert data-sub)
   [(rf/subscribe [:datagrid/selected-record-pks-internal id])
    (rf/subscribe [:datagrid/primary-key id])
    (rf/subscribe [:datagrid/sorted-records id data-sub])])
 (fn [pks pk-key records]
   (let [s (->> records (map pk-key) set)]
     (filter #(some #{%} s) pks))))

(rf/reg-sub
 :datagrid/selected-records
 (fn [[_ id data-sub] _]
   (assert data-sub)
   [(rf/subscribe [:datagrid/selected-record-pks-internal id])
    (rf/subscribe [:datagrid/primary-key id])
    (rf/subscribe [:datagrid/sorted-records id data-sub])])
 (fn [[pks pk-key records] _]
   (filter #(some #{(get % pk-key)} pks)
           records)))

(rf/reg-sub
 :datagrid/record-selected?
 (fn [[_ id _]]
   [(rf/subscribe [:datagrid/primary-key id])
    ;;TODO this might need selected-record-pks id
    (rf/subscribe [:datagrid/selected-record-pks-internal id])])
 (fn [[pk selected-pks] [_ id record]]
   (some #{(get record pk)} selected-pks)))

(rf/reg-sub
 :datagrid/expanded?
 (fn [db [_ id]]
   (get-in db [:datagrid/data id :expanded?])))

(rf/reg-sub
 :datagrid/edit-rows
 (fn [db [_ id]]
   (get-in db [:datagrid/data id :edit-rows])))

(rf/reg-sub
 :datagrid/show-sure?
 (fn [db [_ id]]
   (get-in db [:datagrid/data id :show-sure?])))

(rf/reg-sub
 :datagrid/header-filter-values
 (fn [db [_ id]]
   (get-in db [:datagrid/data id :header-filter-values])))

(rf/reg-sub
 :datagrid/header-filter-value
 (fn [db [_ id k]]
   (get-in db [:datagrid/data id :header-filter-values k])))


(rf/reg-sub
 :datagrid/always-false
 (fn [db _]
   false))

(rf/reg-sub
 :datagrid/loading?
 (fn [[_ sub]]
   (rf/subscribe (or sub [:datagrid/always-false])))
 (fn [v]
   (js/console.log v)
   v))

(rf/reg-sub
 :datagrid/history
 (fn [db _]
   ;;(cljs.pprint/pprint {::history (get-in db [:datagrid/history])})
   (get-in db [:datagrid/history])))

(rf/reg-sub
 :datagrid/history-by-grid
 (fn [[_ grid-id]]
   ;;(cljs.pprint/pprint {::history-by-grid-1 grid-id})
   (rf/subscribe [:datagrid/history]))
 (fn [history [_ grid-id]]
   ;;(cljs.pprint/pprint {::history-by-grid-2 [grid-id history]})
   (vec (remove #(= grid-id %) history))))
