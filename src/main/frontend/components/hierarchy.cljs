(ns frontend.components.hierarchy
  (:require [frontend.search :as search]
            [clojure.string :as string]
            [frontend.components.block :as block]
            [rum.core :as rum]
            [frontend.ui :as ui]))

(defn get-relation
  ([page]
   (get-relation page 100))
  ([page limit]
   (->> (search/page-search page limit)
        (filter #(or
                  (= page %)
                  (string/starts-with? % (str page "/"))
                  (string/includes? % (str "/" page "/"))
                  (string/ends-with? % (str "/" page))))
        (map #(string/split % #"/"))
        (remove #(= % [page])))))

(rum/defc structures
  [page]
  (let [namespaces (get-relation page)]
    (when (seq namespaces)
      [:div.page-hierachy.mt-6
       (ui/foldable
        [:h2.font-bold.opacity-30 "Hierarchy"]
        [:ul.namespaces {:style {:margin "12px 24px"}}
         (for [namespace namespaces]
           [:li.my-2
            (->>
             (for [page namespace]
               (when (and (string? page) page)
                 (block/page-reference false page {} nil)))
             (interpose [:span.mx-2.opacity-30 "/"]))])]
        true)])))
