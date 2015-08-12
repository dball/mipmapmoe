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

(defn position
  [event]
  [(.-clientX event) (.-clientY event)])

(defn mouse-down
  [event]
  (let [{:keys [canvas]} @scene]
    (set! (.-cursor (.-style canvas)) "move")
    (swap! scene assoc
           :drag (mapv (partial * -1) (position event)))))

(defn mouse-up
  [event]
  (let [{:keys [canvas center drag]} @scene]
    (when drag
      (set! (.-cursor (.-style canvas)) "default")
      (swap! scene assoc
             :center (mapv + center drag (position event))
             :drag nil))))

(defn mouse-move
  [event]
  (let [{:keys [canvas center drag]} @scene]
    (when drag
      (let [[left top] (mapv + center drag (position event))]
        (set! (.-left (.-style canvas)) left)
        (set! (.-top (.-style canvas)) top)))))

(defn init!
  []
  (let [canvas (.getElementById js/document "map")
        viewport (.getElementById js/document "viewport")
        center [-400 -300]]
    (swap! scene assoc :canvas canvas :center center)
    (.addEventListener viewport "mousedown" mouse-down)
    (.addEventListener viewport "mouseup" mouse-up)
    (.addEventListener viewport "mousemove" mouse-move)
    (.addEventListener viewport "mouseout" mouse-up)))

(.addEventListener js/window "load" (comp draw! init!))
