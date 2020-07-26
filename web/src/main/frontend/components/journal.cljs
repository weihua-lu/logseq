(ns frontend.components.journal
  (:require [rum.core :as rum]
            [frontend.util :as util :refer-macros [profile]]
            [frontend.date :as date]
            [frontend.handler :as handler]
            [frontend.db :as db]
            [frontend.state :as state]
            [clojure.string :as string]
            [frontend.ui :as ui]
            [frontend.format :as format]
            [frontend.components.content :as content]
            [frontend.components.hiccup :as hiccup]
            [frontend.components.reference :as reference]
            [frontend.components.page :as page]
            [frontend.utf8 :as utf8]
            [goog.object :as gobj]
            [clojure.string :as string]))

(defn- journal-include-template!
  [state]
  (let [[[title format]] (:rum/args state)
        page (string/lower-case title)
        today? (= page (string/lower-case (date/journal-name)))
        repo (state/get-current-repo)]
    ;; no contents yet
    (when today?
      (let [raw-headings (db/get-page-headings repo page)
            headings (db/with-dummy-heading raw-headings format nil true)]
        (when (= 1 (count raw-headings))
          (when-let [template (state/get-journal-template)]
            (handler/insert-new-heading!
             (first headings)
             template
             false
             nil
             true))))))
  state)

(rum/defc headings-cp < rum/reactive
  [repo page encoded-page-name format]
  (let [raw-headings (db/get-page-headings repo page)
        headings (db/with-dummy-heading raw-headings format nil true)]
    (content/content
     encoded-page-name
     {:hiccup (hiccup/->hiccup headings
                               {:id encoded-page-name
                                :start-level 2}
                               {})})))

(rum/defc journal-cp < rum/reactive
  {:init journal-include-template!
   :did-update journal-include-template!}
  [[title format]]
  (let [;; Don't edit the journal title
        page (string/lower-case title)
        repo (state/sub :git/current-repo)
        encoded-page-name (util/encode-str page)
        today? (= (string/lower-case title)
                  (string/lower-case (date/journal-name)))]
    [:div.flex-1
     (ui/foldable
      [:a.initial-color {:href (str "/page/" encoded-page-name)
                         :on-click (fn [e]
                                     (util/stop e)
                                     (when (gobj/get e "shiftKey")
                                       (when-let [page (db/pull [:page/name title])]
                                         (state/sidebar-add-block!
                                          (state/get-current-repo)
                                          (:db/id page)
                                          :page
                                          {:page page
                                           :journal? true}))
                                       (handler/show-right-sidebar)))}
       [:h1.title
        (util/capitalize-all title)]]

      (headings-cp repo page encoded-page-name format))

     (page/today-queries repo today? false)

     (reference/references title false)]))

(rum/defc journals <
  [latest-journals]
  [:div#journals
   (ui/infinite-list
    (for [[journal-name format] latest-journals]
      [:div.journal.content {:key journal-name}
       (journal-cp [journal-name format])])
    {:on-load (fn []
                (handler/load-more-journals!))})])