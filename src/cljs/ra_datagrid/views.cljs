(ns ra-datagrid.views
  (:require [cljs-time.coerce :as coerce]
            [cljs-time.format :as fmt]
            [cljs.pprint :as pprint]
            [clojure.string :as str]
            [pogonos.core :as pg]
            [ra-datagrid.events]
            [ra-datagrid.schema :as ds]
            [ra-datagrid.subs]
            [ra-datagrid.utils :as utils]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [schema.core :as s
             :include-macros true]))

(defn clean-formatted-keys
  [r]
  (into {}
        (remove
         (fn [[k v]]
           (clojure.string/ends-with? (name k) "-formatted" ))
         r)))

(defn is-checked?
  [pkey record selected-records]
  (let [rs (keys (group-by pkey selected-records))]
    (some #{(pkey record)} rs)))

(defn cell-select-checkbox
  [id record]
  (let [options          (rf/subscribe [:datagrid/options id])
        record-selected? (rf/subscribe [:datagrid/record-selected? id record])]
    (fn [id record]
      (let [pkey (:id-field @options)]
        [:td.check {:key (str "checkbox_" (get record pkey))}
         [:div.checkbox
          [:label
           [:input {:type      :checkbox
                    :checked   (if @record-selected? true false)
                    :on-change #(rf/dispatch [:datagrid/toggle-checkbox id record])}]
           [:i.input-helper]]]]))))

(defn are-you-sure-modal
  [id]
  (let [options (rf/subscribe [:datagrid/options id])]
    (fn [id]
      [:div
       [:div.sweet-overlay.are-you-sure {:style {:display :block}}]
       [:div.sweet-alert.showSweetAlert.visible
        {:style {:display :block} }
        [:div.icon.warning.pulseWarning
         [:span.body.pulseWarningIns]
         [:span.dot.pulseWarningIns]]
        [:h2 (:delete-are-you-sure-title @options)]
        [:p.lead.text-muted (:delete-are-you-sure-message @options)]
        [:p
         [:button.btn.cancel.btn-lg.btn-default
          {:on-click #(rf/dispatch [:datagrid/show-are-you-sure-popup id false])}
          (or (:no-text @options) "No")]
         [:button.btn.btn-lg.confirm.btn-warning
          {:on-click #(rf/dispatch [:datagrid/delete-record id])}
          (or (:yes-text @options) "Yes")]]]])))

(defn create-button
  [id]
  (fn [id]
    [:button.btn.btn-icon.btn-primary
     {:on-click #(rf/dispatch [:datagrid/create-new-record id])}
     [:i.fas.fa-plus-square]]))

(defn save-cell-button
  [grid-id pk]
  (let [*valid? (rf/subscribe [:datagrid/edited-record-valid? grid-id pk])]
    (fn [grid-id pk]
      [:td.commands {:key       "SAVECELLBUTTON"
                     :className "save"}
       [:button.btn.btn-icon.btn-primary.btn-success
        {:on-click #(rf/dispatch [:datagrid/save-edited-record grid-id pk])
         :disabled (false? @*valid?)}
        [:i.fe.fe-check-square]]])))

(defn delete-cell-button
  [id record]
  (fn [id record]
    [:span {:key "DELETE" :className "delete"}
     [:button.btn.btn-xs.btn-danger.waves-effect.waves-circle.waves-float
      {:on-click #(rf/dispatch [:datagrid/delete-record-maybe id record])}
      [:i.zmdi.zmdi-close]]]))

(defn reorder-cell-button-up
  [callback]
  [:span {:key "REORDER" :className "reorder reorder-up"}
   [:button.btn.btn-xs.bgm-gray.waves-effect.waves-circle.waves-float
    {:on-click callback}
    [:i.zmdi.zmdi-long-arrow-up]]])

(defn reorder-cell-button-down
  [callback]
  [:span {:key "REORDER" :className "reorder reorder-down"}
   [:button.btn.btn-xs.bgm-gray.waves-effect.waves-circle.waves-float
    {:on-click callback}
    [:i.zmdi.zmdi-long-arrow-down]]])

(defn edit-cell-button
  "Starts editing the row"
  [id pk record]
  [:span {:key "EDITING" :className "edit"}
    [:button.btn.btn-icon.btn-primary
     {:on-click #(rf/dispatch [:datagrid/start-edit id pk (clean-formatted-keys record)])}
     [:i.fe.fe-edit-3]]])

(defmulti table-header-filter (fn [id field]
                                (:type field)))

(defmethod table-header-filter :default
  [id {:keys [name] :as field}]
  (let [v (rf/subscribe [:datagrid/header-filter-value id name])]
    (fn [id {:keys [name] :as field}]
      [:div.table-header-filter.m-b-10
       [:input.form-control
        {:value       (or @v "")
         :placeholder "Filter..."
         :on-change   #(rf/dispatch [:datagrid/header-filter-value id name (-> % .-target .-value)])
         :on-blur     #(rf/dispatch [:datagrid/header-filter-value id name @v true])
         :type        :text}]])))

(defmethod table-header-filter :number
  [id {:keys [name] :as field}]
  (let [v (rf/subscribe [:datagrid/header-filter-value id name])]
    (fn [id {:keys [name] :as field}]
      [:div.table-header-filter.m-b-10
       [:input.form-control
        {:value       (or @v "")
         :placeholder "Filter..."
         :on-change   #(rf/dispatch [:datagrid/header-filter-value id name (-> % .-target .-value )])
         :on-blur     #(rf/dispatch [:datagrid/header-filter-value id name @v true])
         :type        :number}]])))

(defn table-header-cell
  [grid-id {:keys [title align width can-sort hide-header-filter] :as field}]
  (let [align   (if-not align "text-left" align)
        atts    {:className align
                 :key (name (:name field))}
        sorting (rf/subscribe [:datagrid/sorting grid-id])
        options (rf/subscribe [:datagrid/options grid-id])
        ctx     (when-let [ctx-sub (:context-subscription @options)]
                  @(rf/subscribe ctx-sub))]
    (fn [grid-id {:keys [title align width can-sort menu edit]
                  :as   field}]
      (let [align-left?      (= align "text-left")
            sort-by-key      (:key @sorting)
            sort-direction   (:direction @sorting)
            can-sort-global? (:can-sort @options)
            header-filters?  (:header-filters @options)
            title            (if (and (not-empty ctx)
                                      (str/includes? title "{{"))
                               (pg/render-string title ctx)
                               #_else title)]
        [:th atts
         [:div.btn-toolbar {:role "toolbar"}
          [:div {:class ["btn-group" (if align-left? "mr2" "ml2")]
                 :role  "group"}
           [:a.btn.btn-link {:type                    "button"
                             :style                   (if align-left?
                                                        {:padding-left 0}
                                                        {:padding-right 0})
                             :dangerouslySetInnerHTML {:__html title}}]
           (when (and can-sort-global?
                      (not= false can-sort))
             [:a.btn.btn-link {:type "button"} [:i.fas.fa-arrows-alt-v]])
           (when menu
             [:div.dropdown
              [:a.btn.btn-link {:data-toggle   "dropdown"
                                :aria-expanded "false"
                                :type          "button"} [:i.fas.fa-ellipsis-h.rotate-90]]
              [:div.dropdown-menu.dropdown-menu-right
               (for [{:keys [title view]} menu]
                 ^{:key title}
                 [:button.dropdown-item {:type     "button"
                                         :on-click #(rf/dispatch [:push-state view])}
                  title])
               #_[:a.dropdown-item {:href "#"} "Edit"]]])]]

         #_(if (and can-sort-global?
                  (not= false can-sort))
           [:a.column-header-anchor

            [:span.text.m-r-5
             {:style    {:cursor :pointer}
              :on-click #(rf/dispatch [:datagrid/sort-field grid-id (:name field)])}
             title]
            (cond
              (and
               (= (:name field) sort-by-key)
               (= sort-direction :asc))  [:i.zmdi.zmdi-caret-up]
              (and
               (= (:name field) sort-by-key)
               (= sort-direction :desc)) [:i.zmdi.zmdi-caret-down]

              :else
              [:i {:style {:display :inline-block
                           :width   "5px"
                           :height  "5px"}}])]
           ;;else
           [:span
            [:span.text.m-r-5 title]
            (cond
              (and
               (= (:name field) sort-by-key)
               (= sort-direction :asc))  [:i.zmdi.zmdi-caret-up]
              (and
               (= (:name field) sort-by-key)
               (= sort-direction :desc)) [:i.zmdi.zmdi-caret-down]

              :else
              [:i {:style {:display :inline-block
                           :width   "5px"
                           :height  "5px"}}])])

         (when (and header-filters?
                    (or (nil? hide-header-filter)
                        (not hide-header-filter)))
           [table-header-filter grid-id field])

         (when (and header-filters? hide-header-filter)
           [:div.m-b-10 {:style {:height "35px"}} " "])]))))

(defn mass-select
  [id data-sub]
  (let [visible-records (rf/subscribe [:datagrid/sorted-records id data-sub])
        checked?        (rf/subscribe [:datagrid/mass-select-checked? id])]
    (fn
      [id data-sub]
      [:div.checkbox
       [:label
        [:input {:type      :checkbox
                 :checked   (if @checked? true false)
                 :on-change #(rf/dispatch [:datagrid/toggle-mass-select id @visible-records])}]
        [:i.input-helper]]])))

(defn table-header
  [grid-id data-sub]
  (let [fields           (rf/subscribe [:datagrid/fields grid-id])
        sorting          (rf/subscribe [:datagrid/sorting grid-id])
        options          (rf/subscribe [:datagrid/options grid-id])
        selected-records (rf/subscribe [:datagrid/selected-record-pks grid-id data-sub])]
    (fn [grid-id data-sub]
      (let [cells (map (fn [f]
                         ^{:key (:name f)}
                         [table-header-cell grid-id f]) @fields)
            cells
            (cond->> cells
              (:checkbox-select @options)
              (concat [^{:key "check"}
                       [:th.check
                        {:key "check"}
                        [mass-select grid-id data-sub]]]))]
        [:thead  {:key "head"}
         (when (:extra-header-row @options)
           (:extra-header-row @options))
         (when-not (:hide-heading @options)
           [:tr
            (if (:can-create @options)
              (concat [ ^{:key "cmds"}
                       [:th.commands
                        [create-button grid-id]]] cells)
              cells)])]))))

(defmulti edit-cell
  (fn [_ {t :type} _]
    (or t :string)))

(defn invalid-feedback
  [feedback]
  [:div.invalid-tooltip
   [:<>
    (for [f feedback]
      ^{:key (js/encodeURI "edit-cell-validation" f)}
      [:span f])]])

(defmethod edit-cell :number
  [grid-id field pk]
  (let [*r (rf/subscribe [:datagrid/edited-record-by-pk-with-validation grid-id pk])]
    (fn [grid-id field pk]
      (let [field-nm   (:name field)
            validation (-> @*r :validation (get field-nm))
            v          (-> @*r :result (get field-nm))]
        [:td {:key       (:name field)
              :className "editing data-cell"}
         [:div
          [:input.form-control {:type      "number"
                                :value     v
                                :class     (when (seq validation)
                                             "is-invalid")
                                :on-change #(rf/dispatch [:datagrid/update-edited-record grid-id pk
                                                          (:name field) (-> %
                                                                            .-target
                                                                            .-value
                                                                            cljs.reader/read-string)])}]
          (invalid-feedback validation)]]))))

(defmethod edit-cell :custom
  [id field pk]
  (let [r (rf/subscribe [:datagrid/edited-record-by-pk id pk])]
    (fn [id field pk]
      (let [v (get @r (:name field))]
        [:td {:key (:name field)
              :className "editing data-cell"}
         ((:custom-element-edit-renderer field) field @r
          #(rf/dispatch [:datagrid/update-edited-record id pk (:name field) %]) v)]))))

(defmethod edit-cell :yesno
  [id field pk]
  (let [r (rf/subscribe [:datagrid/edited-record-by-pk id pk])]
    (fn [id field pk]
      (let [v (get @r (:name field))]
        [:td {:key       (:name field)
              :className "editing"}
         [:div.fg-line
          [:div.select
           [:select.form-control
            {:value     (if v "true" "false")
             :on-change #(rf/dispatch [:datagrid/update-edited-record id pk
                                       (:name field) (= "true" (.-target.value ^js %))])}
            [:option {:value "true"}  "Yes"]
            [:option {:value "false"} "No"]]]]]))))

(defmethod edit-cell :no-edit
  [id field pk]
  (let [r (rf/subscribe [:datagrid/edited-record-by-pk id pk])]
    (fn [id field pk]
      (let [v (get @r (:name field))]
        [:td {:key       (:name field)
              :className "editing data-cell"}
         (if (:formatter field)
           ((:formatter field) v @r)
           v)]))))

(defmethod edit-cell :empty-edit
  [_  {:keys [name]} _]
  (fn [_ {:keys [name]} _]
    [:td {:key       name
          :className "editing"}]))

(defmethod edit-cell :default
  [id field pk]
  (let [r (rf/subscribe [:datagrid/edited-record-by-pk id pk])]
    (fn [id field pk]
      (let [v (get @r (:name field))]
        [:td {:key       (:name field)
              :className "editing data-cell"}
         [:div.fg-line
          [:input.form-control {:type      "text"
                                :value     v
                                :on-change #(rf/dispatch [:datagrid/update-edited-record id pk
                                                          (:name field) (.-target.value ^js %)])}]]]))))

(defn create-row
  [id]
  (let []
    (fn [id]
      [:div "TODO"]
      )))

(defn edit-row
  "shows a row with inline editing elements"
  [grid-id pk]
  (let [options (rf/subscribe [:datagrid/options grid-id])
        fields  (rf/subscribe [:datagrid/fields grid-id])]
    (fn [grid-id pk]
      (let [{:keys [checkbox-select]} @options
            save-button               ^{:key (or pk -1)} [save-cell-button grid-id pk]
            cells                     (cond->> (doall
                                                (map (fn [f]
                                                       ^{:key (:name f)}
                                                       [edit-cell grid-id f pk]) @fields))
                                        checkbox-select
                                        (concat [^{:key "checkbox__"}
                                                 [edit-cell grid-id {:name (str "checkbox-" grid-id)
                                                                :type :empty-edit}]]))]
        [:tr.editing {:key pk} (concat [save-button] cells)]))))


(defmulti table-cell
  (fn [_ {t :type} _ _]
    (or t :string)))

(defmethod table-cell :custom
  [id field record first?]
  (let [is-clickable? (not (nil? (:on-click field)))
        fieldname     (:name field)
        fmt-fieldname (-> field :name name (str "-formatted") keyword)
        value         (or
                       (get record fmt-fieldname)
                       (get record fieldname))
        align         (if (nil? (:align field)) "text-left" (:align field))
        style         (if-let [w (:width field)]
                        {:min-width w} #_else {})]
    [:td {:key (:name field), :className align, :style style}
     [:span (cond-> {}
              is-clickable? (assoc :on-click #((:custom-element-click field) record)))
      [(:custom-element-renderer field) record]]]))

(defmethod table-cell :default
  [id field record indent?]
  (let [options (rf/subscribe [:datagrid/options id])]
    (fn [id field record indent?]
      (let [is-clickable?   (not (nil? (:on-click field)))
            formatter       (:formatter field)
            fieldname       (:name field)
            fmt-fieldname   (-> field :name name (str "-formatted") keyword)
            formatted-value (or
                             (get record fmt-fieldname)
                             (get record fieldname))
            align           (if (nil? (:align field)) "text-left" (:align field))
            formatted-value (if is-clickable?
                              [:a.table-link {:on-click
                                              (fn [e]
                                                (let [f (:on-click field)]
                                                  (f (clean-formatted-keys record)
                                                     field e
                                                     @options)))}
                               formatted-value]
                              formatted-value)]
        [:td (cond-> {:key       fieldname
                      :className (str align " data-cell")}
               indent?        (update :style #(assoc % :padding-left "30px"))
               (:width field) (update :style #(assoc % :min-width (:width field))))
         formatted-value]))))

(defn command-td
  [id
   {:keys [can-edit can-edit-fn can-reorder can-reorder-fn-up
           id-field can-reorder-fn-down can-delete can-delete-fn data-subscription]
    :as   options}
   record]
  (let [sorted-records (rf/subscribe [:datagrid/sorted-records id data-subscription])]
    (fn [id
         {:keys [can-edit can-edit-fn can-reorder can-reorder-fn-up
                 id-field can-reorder-fn-down can-delete can-delete-fn data-subscription]
          :as   options}
         record]
      [:td.commands
       (when
           (and can-edit
                (or
                 (nil? can-edit-fn)
                 (and
                  (not (nil? can-edit-fn))
                  (can-edit-fn record))))
         [edit-cell-button id (get record id-field) record])

       (when (and can-reorder
                  (or (nil? can-reorder-fn-up)
                      (can-reorder-fn-up record @sorted-records)))
         [reorder-cell-button-up #(rf/dispatch [:datagrid/reorder id :up (clean-formatted-keys record)])])

       (when (and can-reorder
                  (or (nil? can-reorder-fn-down)
                      (can-reorder-fn-down record @sorted-records)))
         [reorder-cell-button-down #(rf/dispatch [:datagrid/reorder id :down (clean-formatted-keys record)])])

       (when
           (and can-delete
                (or
                 (nil? can-delete-fn)
                 (and
                  (not (nil? can-delete-fn))
                  (can-delete-fn record))))
         [delete-cell-button id record])])))

(defn non-edit-row
  [id record]
  (let [options (rf/subscribe [:datagrid/options id])
        fields  (rf/subscribe [:datagrid/fields id])]
    (fn [id record]
      (let [group-by            (some (fn [{:keys [type name]}]
                                        (when (= type :indent-group) name))
                                      @fields)
            {:keys
             [id-field
              checkbox-select]} @options
            pk                  (get record id-field)
            k                   (if (or (= "" pk) (nil? pk))
                                  "editing"
                                  pk)

            classNames (if (:row-formatter @options)
                         ((:row-formatter @options) record)
                         "")

            classNames (if (:show-max-num-rows @options)
                         (str classNames " " "expandable")
                         classNames)
            atts       (cond-> {:key k :className classNames}
                         ;;(:show-max-num-rows @options) (assoc :on-click (:expand-handler @options)))
                         false (assoc :on-click (:expand-handler @options)))

            atts (if (:on-record-click @options)
                   (assoc atts :on-click #((:on-record-click @options) record @fields @options))
                   atts)

            first-f (->> @fields first :name)

            cells (cond->> (doall
                            (map (fn [{:keys [name] :as f}]
                                   (let [indent? (and group-by
                                                      (= name first-f)
                                                      (get record group-by))]
                                     ^{:key name}
                                     [table-cell id f record indent?])) @fields))
                    checkbox-select
                    (concat [^{:key "checkbox__"}
                             [cell-select-checkbox id record]]))]
        [:tr atts
         (cond->> cells
           (or (:can-update @options) (:can-edit @options) (:can-delete @options))
           (concat [^{:key "commands"}
                    [command-td id @options record]]))]))))

(defn table-row
  [id record]
  (let [options  (rf/subscribe [:datagrid/options id])
        editing? (rf/subscribe [:datagrid/editing-record? id record])]
    (fn [id record]
      (if @editing?
        ^{:key ((:id-field @options) record)}
        [edit-row id (get record (:id-field @options))]

        ^{:key ((:id-field @options) record)}
        [non-edit-row id record]))))

(defn table-data
  [id data-sub]
  (let [expanded?       (rf/subscribe [:datagrid/expanded? id])
        options         (rf/subscribe [:datagrid/options id])
        fields          (rf/subscribe [:datagrid/fields id])
        creating?       (rf/subscribe [:datagrid/creating? id])
        all-records     (rf/subscribe [:datagrid/records data-sub])
        visible-records (rf/subscribe [:datagrid/sorted-records id data-sub])]
    (fn [id data-sub]
      (let [rows     (doall
                      (map (fn [r]
                             ^{:key ((:id-field @options) r)}
                             [table-row id r])
                           @visible-records))
            max-rows (:show-max-num-rows @options)]
        [:tbody {:key "body"}
         (cond-> rows
           @creating? (conj ^{:key -9}
                            [edit-row id nil])

           (and max-rows
                (not @expanded?)
                (> (count @all-records) max-rows))
           , (concat [^{:key -2}
                      [:tr
                       [:td
                        [:button.btn.btn-primary
                         {:on-click #(rf/dispatch [:datagrid/toggle-expand id])} "Show more"]]]])

           (and max-rows
                @expanded?
                (> (count @all-records) max-rows))
           , (concat [^{:key -3}
                      [:tr
                       [:td
                        [:button.btn.btn-primary
                         {:on-click #(rf/dispatch [:datagrid/toggle-expand id])} "Show less"]]]]))]))))

(defn table-footer
  [id fields records]
  (let []
    [:tfoot
     [:tr
      (doall
       (map-indexed
        (fn [i f]
          ^{:key i}
          [:td
           (when (:footer-cell f)
             ((:footer-cell f) records))])
        fields))]]))

(s/defn ^:always-validate datagrid
  "Creates a datagrid"
  [options :- ds/GridConfiguration
   fields  :- [ds/GridField]]
  (let [id              (:grid-id options)
        data-sub        (:data-subscription options)
        loading-sub     (:loading-subscription options)
        creating?       (rf/subscribe [:datagrid/creating? id])
        show-sure?      (rf/subscribe [:datagrid/show-sure? id])
        ;; we do this aliassing, since we have 'sorted-records' depend on this one
        records         (rf/subscribe [:datagrid/records data-sub])
        current-options (rf/subscribe [:datagrid/options id])
        current-fields  (rf/subscribe [:datagrid/fields id])
        initialized?    (rf/subscribe [:datagrid/initialized? id])
        loading?        (rf/subscribe [:datagrid/loading? loading-sub])]
    (fn [options fields]
      (if-not @initialized?
        (do (rf/dispatch [:datagrid/initialize options fields])
            [:div.p-30
             {:style {:text-align :center}}
             [:div.preloader.pl-xl
              [:svg.pl-circular
               {:viewBox "25 25 50 50"}
               [:circle.plc-path {:r "20", :cy "50", :cx "50"}]]]])
        ;;else
        (let [colspan (cond-> (count fields)
                        (:checkbox-select options) inc)]
          (when (not= options @current-options)
            (rf/dispatch [:datagrid/update-options id options]))
          (when (not= fields @current-fields)
            (rf/dispatch [:datagrid/update-fields id fields]))
          [:div.full-height
           (when @show-sure?
             [are-you-sure-modal id])
           [:div.table-responsive
            [:table.table.bootgrid-table
             {:class (str (name id) " " (:additional-css-class-names options))}
             (when-not (and (:hide-heading options)
                            (not (:extra-header-row options)))
               [table-header id data-sub])
             (when-not (empty?
                        (filter (fn [f]
                                  (not (nil? (:footer-cell f)))) fields))
               [table-footer id fields @records])
             (cond
               @loading?
               , [:tbody
                  [:tr
                   [:td {:col-span (count fields)}
                    [:div.p-30
                     {:style {:text-align :center}}
                     [:div.preloader.pl-xl
                      [:svg.pl-circular
                       {:viewBox "25 25 50 50"}
                       [:circle.plc-path {:r "20", :cy "50", :cx "50"}]]]]]]]

               (and (empty? @records) (not @creating?))
               , [:tbody
                  [:tr
                   [:td.nodata {:style    {:padding-top "20px"}
                                :col-span colspan}
                    [:i (or (:no-records-text options) "No rows")]]]]

               :else
               , [table-data id (:data-subscription options)])]
            (when (:progressive-loading options)
              [:div.re-datagrid-read-more-marker])]])))))
