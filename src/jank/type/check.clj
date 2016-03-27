(ns jank.type.check
  (:require [jank.type.declaration :as declaration]
            [jank.type.binding :as binding]
            [jank.type.expression :as expression]
            [jank.type.return :as return])
  (:use clojure.pprint
        clojure.tools.trace
        jank.assert))

; XXX: migrated
(defmulti check-item
  "Type checks the given expression. Returns a cons of the typed
   expression and the updated scope."
  (fn [item scope]
    (:kind item)))

; XXX: migrated
(defn empty-scope
  "Builds an empty type scope."
  ([]
   (empty-scope nil))
  ([parent]
   {:parent parent
    :binding-declarations {}
    :binding-definitions {}
    :type-declarations #{}}))

; XXX: migrated
(defn check
  "Builds type information on the parsed source. Returns
   a list of the typed source and the top-level scope."
  ([parsed]
   (check parsed (empty-scope)))
  ([parsed parent-scope]
   ;(pprint (list "parsed:" parsed))
   (loop [item (first (:cells parsed))
          remaining (rest (:cells parsed))
          checked-cells []
          scope parent-scope]
     ;(pprint (list "scope:" scope))
     (if (nil? item)
       (assoc parsed
              :cells checked-cells
              :scope scope)
       (let [checked-item (check-item item scope)]
         (recur (first remaining)
                (rest remaining)
                (conj checked-cells checked-item)
                (:scope checked-item)))))))

; XXX: migrated | tested
(defmethod check-item :declare-statement
  [item scope]
  (assoc item :scope (declaration/add-to-scope item scope)))

; XXX: migrated | tested
(defmethod check-item :lambda-definition
  [item scope]
  (let [args (:arguments item)
        return (:return item)
        new-scope (empty-scope scope)
        checked-args (check-item args new-scope)
        checked-return (check-item return (:scope checked-args))
        checked-body (check {:cells (:body item)} (:scope checked-return))
        updated-item (assoc item :body (:cells checked-body))
        item-with-return (return/add-explicit-returns updated-item
                                                      (:scope checked-body))]
    (assoc item-with-return
           :arguments checked-args
           :scope scope)))

; XXX: migrated | tested
(defmethod check-item :binding-definition
  [item scope]
  ; There is an optional type specifier which may be before the value
  (let [checked-name (check-item (:name item) scope)
        value (:value item)
        checked-val (check-item
                      value
                      ; Add a declaration before checking it. This allows
                      ; recursive functions to have a declaration of
                      ; themselves.
                      (if (= :lambda-definition (:kind value))
                        (declaration/add-to-scope
                          (assoc item
                                 :type (expression/realize-type value scope))
                          scope)
                        scope))
        updated-item (assoc item
                            :name checked-name
                            :value checked-val
                            :type (expression/realize-type
                                    checked-val
                                    (:scope checked-val)))]
    (assoc updated-item
           :scope (binding/add-to-scope updated-item (:scope checked-val)))))

; Check the type of each argument and try to realize the resulting
; function type.
; XXX: migrated
(defmethod check-item :function-call
  [item scope]
  (loop [args (:arguments item)
         checked-args []
         new-scope scope]
    (if (empty? args)
      (let [checked-name (check-item (:name item) scope)
            args-with-returns (map #(return/add-parameter-returns % new-scope)
                                   checked-args)
            updated-item (assoc item
                               :name checked-name
                               :arguments args-with-returns)
            signature (expression/call-signature updated-item new-scope)]
        (assoc updated-item
               :scope new-scope
               :signature signature))
      (let [checked-arg (check-item (first args) new-scope)]
        (recur (rest args)
               (conj checked-args checked-arg)
               (:scope checked-arg))))))

; Bring the arguments into scope and type check.
; XXX: migrated | tested
(defmethod check-item :argument-list
  [item scope]
  ; Group arg names and types; pull out only the names and verify
  ; they're all distinct.
  (let [args (partition 2 (:values item))]
    (when (not-empty args)
      (type-assert (distinct (map first args))
                   "not all parameter names are distinct"))
    (assoc item
           :scope
           (loop [remaining args
                  new-scope scope]
             (if (empty? remaining)
               new-scope
               (recur (rest remaining)
                      (declaration/add-to-scope
                        {:kind :binding-declaration
                         :name (ffirst remaining)
                         :type (second (first remaining))}
                        new-scope)))))))

; XXX: migrated | tested
(defmethod check-item :return-list
  [item scope]
  (let [returns (count (:values item))]
    (type-assert (<= returns 1) "multiple return types")
    (if (> returns 0)
      (let [expected-type (declaration/lookup-type
                            (first (:values item))
                            scope)]
        (type-assert expected-type "invalid return type")
        (assoc item
               :type expected-type
               :scope scope))
      (assoc item :scope scope))))

; XXX: migrated
(defmethod check-item :if-expression
  [item scope]
  (let [cond-type (expression/realize-type (:value (:condition item)) scope)]
    (type-assert (= cond-type {:kind :type
                               :value {:kind :identifier :name "boolean"}})
                 (str "if expression condition must be boolean, not " cond-type))
    (let [checked-then (check {:cells [(:value (:then item))]}
                              (empty-scope scope))
          updated-item (assoc item :then checked-then)]
      (if (contains? item :else)
        (let [checked-else (check {:cells [(:value (:else updated-item))]}
                                  (empty-scope scope))]
          (assoc updated-item
                 :else checked-else
                 :scope scope))
        (assoc updated-item :scope scope)))))

; XXX: migrated
(defmethod check-item :list [item scope]
  (assoc item :scope scope))

; XXX: migrated
(defmethod check-item :string [item scope]
  (assoc item :scope scope))

; XXX: migrated
(defmethod check-item :integer [item scope]
  (assoc item :scope scope))

; XXX: migrated
(defmethod check-item :real [item scope]
  (assoc item :scope scope))

; XXX: migrated
(defmethod check-item :boolean [item scope]
  (assoc item :scope scope))

; XXX: migrated
(defmethod check-item :identifier [item scope]
  (assoc item :scope scope))

; XXX: migrated
(defmethod check-item :default [item scope]
  (type-assert false (str "no type checking for '" item "'")))
