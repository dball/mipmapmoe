(ns db.mmm
  (:require [monet.canvas :as canvas]))

(defmacro with-style
  [context & body]
  `(do
     (canvas/save ~context)
     (try
       ~@body
       (finally
         (canvas/restore ~context)))))

(def scene
  (atom {}))

(defn draw!
  []
  (let [{:keys [canvas center scale]} @scene
        width (.-width canvas)
        height (.-height canvas)
        context (canvas/get-context canvas "2d")]
    (println "draw!" scale)
    (with-style context
      (canvas/transform context scale 0 0 scale (/ width 2) (/ height 2))
      (canvas/stroke-style context "#eee")
      (canvas/stroke-width context 0.1)
      (doseq [radius (range 1 50)]
        (canvas/circle context {:x 0 :y 0 :r radius})
        (canvas/stroke context)))))

(defn position
  [event]
  [(.-clientX event) (.-clientY event)])

(defn parse-pixels
  [s]
  (when-let [[_  pixels] (re-find #"(-?\d+)px" s)]
    pixels))

(defn mouse-down
  [event]
  (let [{:keys [canvas]} @scene
        canvas-offset (mapv parse-pixels [(.-left (.-style canvas))
                                          (.-top (.-style canvas))])
        drag-offset (position event)
        offset (mapv - canvas-offset drag-offset)]
    (set! (.-cursor (.-style canvas)) "move")
    (swap! scene assoc
           :drag-fn (fn [event]
                      (set! (.-cursor (.-style canvas)) "default")
                      (let [[left top] (mapv + offset (position event))]
                        (set! (.-left (.-style canvas)) left)
                        (set! (.-top (.-style canvas)) top))))))

(defn mouse-up
  [event]
  (swap! scene dissoc :drag-fn))

(defn mouse-move
  [event]
  (let [{:keys [drag-fn]} @scene]
    (when drag-fn
      (drag-fn event))))

(defn init!
  []
  (enable-console-print!)
  (println "init!")
  (let [viewport (.getElementById js/document "viewport")
        canvas (.getElementById js/document "map")
        width (.-clientWidth viewport)
        height (.-clientHeight viewport)]
    (set! (.-width canvas) (* 3 width))
    (set! (.-height canvas) (* 3 height))
    (set! (.-left (.-style canvas)) (* -1 width))
    (set! (.-top (.-style canvas)) (* -1 height))
    (.addEventListener viewport "mousedown" mouse-down)
    (.addEventListener viewport "mouseup" mouse-up)
    (.addEventListener viewport "mousemove" mouse-move)
    (.addEventListener viewport "mouseout" mouse-up)
    (swap! scene assoc
           :canvas canvas
           :center [0 0]
           :scale 10)))

(.addEventListener js/window "load" (comp draw! init!))
