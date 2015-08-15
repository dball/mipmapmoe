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
  (let [{:keys [canvas center draws scale]} @scene
        [left-offset top-offset] center
        width (.-width canvas)
        height (.-height canvas)
        offscreen-canvas (.createElement js/document "canvas")]
    (set! (.-left (.-style canvas)) (/ width -3))
    (set! (.-top (.-style canvas)) (/ height -3))
    (set! (.-width offscreen-canvas) width)
    (set! (.-height offscreen-canvas) height)
    (let [context (canvas/get-context offscreen-canvas "2d")]
      (with-style context
        (canvas/transform context scale 0 0 scale
                          (+ left-offset (/ width 2))
                          (+ top-offset (/ height 2)))
        (canvas/stroke-style context "#eee")
        (canvas/stroke-width context 0.1)
        (doseq [radius (range 1 (* (inc draws) 5))]
          (canvas/circle context {:x 0 :y 0 :r radius})
          (canvas/stroke context))))
    (let [context (canvas/get-context canvas "2d")]
      (canvas/clear-rect context {:x 0 :y 0 :w width :h height})
      (canvas/draw-image context offscreen-canvas 0 0)))
  (swap! scene update :draws inc)
  (println @scene))

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
                        (set! (.-top (.-style canvas)) top))
                      (mapv - (position event) drag-offset)))))

(defn mouse-up
  [event]
  (let [{:keys [drag-fn]} @scene]
    (when drag-fn
      (let [dragged (drag-fn event)]
        (println "dragged" dragged)
        (swap! scene (fn [scene]
                       (-> scene
                           (dissoc :drag-fn)
                           (update :center (partial mapv +) dragged))))
        (draw!)))))

(defn mouse-move
  [event]
  (let [{:keys [drag-fn]} @scene]
    (when drag-fn
      (drag-fn event))))

(defn mouse-wheel
  [event delta]
  (let [operator (if (pos? (.-wheelDelta event)) * /)]
    (swap! scene update :scale operator 1.2)
    (draw!)))

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
    (.addEventListener viewport "mousedown" mouse-down)
    (.addEventListener viewport "mouseup" mouse-up)
    (.addEventListener viewport "mousemove" mouse-move)
    (.addEventListener viewport "mouseout" mouse-up)
    (.addEventListener viewport "mousewheel" mouse-wheel)
    (swap! scene assoc
           :canvas canvas
           :center [0 0]
           :scale 10
           :draws 0)))

(.addEventListener js/window "load" (comp draw! init!))
