(ns reason-alpha.views.main
  (:require [re-frame.core :as rf]))

(defn view [child-view]
  [:div.page-main
   [:div#headerMenuCollapse.ren-navbar>div.container
    [:ul.nav
     [:li
      [:a.nav-link {:data-toggle   "dropdown"
                    :href          "#"
                    :aria-expanded "true"}
       [:i.fas.fa-cog]
       [:span " "]]
      [:div.dropdown-menu.dropdown-menu-right.dropdown-menu-arrow
       {;:x-placement "bottom-end"
        :style       {:position    "absolute"
                      :transform   "translate3d(-32px, 46px, 0px)"
                      :top         "0px"
                      :left        "0px"
                      :will-change "transform"}}
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
       [:i.fas.fa-exchange-alt] [:span "TRADING"]]]
     [:li.nav-item
      [:a.nav-link {:href "#"}
       [:i.fas.fa-exchange-alt] [:span "ANALYSIS"]]]
     [:li.nav-item
      [:a.nav-link {:href "#"}
       [:i.fas.fa-exchange-alt] [:span "IMPORT"]]]
     [:li.nav-item
      [:a.nav-link {:href "#"}
       [:i.fas.fa-exchange-alt] [:span "ADD"]]]
     [:li.nav-item
      [:a.nav-link {:href "#"}
       [:i.fas.fa-exchange-alt] [:span "DELETE"]]]
     [:li.nav-item
      [:a.nav-link {:href "#"
                    :on-click #(rf/dispatch [:save])}
       [:i.fas.fa-exchange-alt] [:span "SAVE"]]]
     [:li.nav-item
      [:a.nav-link {:href "#"}
       [:i.fas.fa-exchange-alt] [:span "CANCEL"]]]]]
   [:div.container.h-5-perc>div.side-app.h-5-perc
    [:div.page-header
     [:h4.page-title "Portfolio Trades"]
     [:ol.breadcrumb>li.breadcrumb-item.active {:aria-current "page"}
      [:a {:href "#"} "Portfolio Trades"]]]
    [:div.row.h-4-perc>div.col-md-12.col-lg-12.h-5-perc
     [:div.card.h-5-perc
      [:div.card-status.bg-yellow.br-tr-3.br-tl-3]
      [:div.card-body.h-5-perc
       [child-view]]]]]])
 
