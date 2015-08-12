(ns db.mmm)

(enable-console-print!)

(def scene
  (atom {}))

(defn draw!
  []
  (let [{:keys [canvas]} @scene
        height (.-height canvas)
        width (.-width canvas)
        context (.getContext canvas "2d")]
    (set! (.-strokeStyle context) "#eee")
    (loop [x 0.5]
      (when (< x width)
        (.moveTo context x 0)
        (.lineTo context x height)
        (.stroke context)
        (recur (+ x 10))))))

(defn init!
  []
  (let [canvas (.getElementById js/document "map")
        center [-400 -300]]
    (swap! scene assoc :canvas canvas :center center)))

(.addEventListener js/window "load" (comp draw! init!))
