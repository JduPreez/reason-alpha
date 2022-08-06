(ns reason-alpha.views.main
  (:require [re-frame.core :as rf]))

(defn view [child-view alerts-view]
  [:div.page-main
   [:div#headerMenuCollapse.ren-navbar>div.container
    [:ul.nav
     [:li
      [:a.nav-link {:data-toggle   "dropdown"
                    :href          "#"
                    :aria-expanded "true"}
       [:i.fas.fa-cog {:style {:height       "18px"
                               :margin-right "0"}}]
       [:span " "]]
      [:div.dropdown-menu ;;.dropdown-menu-right.dropdown-menu-arrow
       [:a.dropdown-item {:href "#"}
        [:i.dropdown-icon.mdi.mdi-account-outline] " Profile"]
       [:a.dropdown-item {:href "#"}
        [:i.dropdown-icon.mdi.mdi-settings] " Settings"]
       [:a.dropdown-item {:href "#"}
        [:span.float-right [:span.badge.badge-primary "6"]]
        [:i.dropdown-icon.mdi.mdi-message-outline] " Inbox"]
       [:a.dropdown-item {:href "#"}
        [:i.dropdown-icon.mdi.mdi-comment-check-outline] " Message"]
       [:div.dropdown-divider]
       [:a.dropdown-item {:href "#"}
        [:i.dropdown-icon.mdi.mdi-compass-outline] " Need help?"]
       [:a.dropdown-item {:href "login.html"}
        [:i.dropdown-icon.mdi.mdi-logout-variant] "Sign out"]]]
     [:li.nav-item
      [:a.nav-link {:href "#"}
       [:i.fas.fa-chart-line] [:span "TRADING"]]]
     [:li.nav-item
      [:a.nav-link {:href "#"}
       [:i.fas.fa-chart-pie] [:span "ANALYSIS"]]]
     [:li.nav-item
      [:a.nav-link {:href "#"}
       [:i.fas.fa-file-import] [:span "IMPORT"]]]
     [:li.nav-item
      [:a.nav-link {:href     "#"
                    :on-click #(do (.preventDefault %)
                                   (rf/dispatch [:add]))}
       [:i.fas.fa-plus-square] [:span "ADD"]]]
     [:li.nav-item
      [:a.nav-link {:href     "#"
                    :on-click #(do (.preventDefault %)
                                   (rf/dispatch [:delete!]))}
       [:i.fas.fa-minus-square] [:span "DELETE"]]]
     [:li.nav-item
      [:a.nav-link {:href "#"}
       [:i.fas.fa-undo-alt] [:span "CANCEL"]]]]]
   [:div.container.full-height>div.hor-content.full-height
    ;;[alerts-view]
    [:div.row.full-height {:style {:margin-top "10px"}}
     [child-view]
     #_[:div.card
      [:div.card-header.bg-gradient-indigo.br-tr-3.br-tl-3
       [:div.btn-list
        [:button.btn.btn-primary {:type "button"} "Trades"]
        [:button.btn.btn-outline-primary "Trade Patterns"]]
       #_[:h2.card-title "Portfolio Trades"]]
      ;;[:div.card-status.bg-yellow.br-tr-3.br-tl-3]
        [:div.card-body {:style {:padding-top    0
                                 :padding-bottom 0}}
       [child-view]]]]]])
