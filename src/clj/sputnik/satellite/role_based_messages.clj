; Copyright (c) Gunnar Völkel. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file epl-v1.0.txt at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns sputnik.satellite.role-based-messages
  (:require
    [sputnik.satellite.node :as node]
    [sputnik.satellite.protocol :as protocol])
  (:use
    [sputnik.satellite.error :only [send-error]]
    [clojure.tools.logging :only [debug]]))


(protocol/defmessage role-request :all role)
(protocol/defmessage role-granted :all role)



(defn- role!
  [remote-node, role]
  (node/set-data remote-node :role role))

(defn role
  [remote-node]
  (node/get-data remote-node :role))


(defn handle-role-request
  [handle-role-assigned-fn, this-node, remote-node, msg]
  (debug (format "Node %s requested role %s." (node/node-info remote-node) (:role msg)))
  (role! remote-node (:role msg))
  (node/send-message remote-node (role-granted-message (:role msg)))
  (when handle-role-assigned-fn
    (handle-role-assigned-fn this-node, remote-node, (:role msg)))
  nil)


(defn handle-message-checked
  ([handle-message-fn, this-node, remote-node, msg]
    (handle-message-checked handle-message-fn, this-node, remote-node, msg, nil))
  ([handle-message-fn, this-node, remote-node, msg, handle-role-assigned-fn]
	  (if (= (type msg) :role-request)
	    (handle-role-request handle-role-assigned-fn, this-node, remote-node, msg)
	    (let [role (role remote-node)]
		    (if (protocol/message-allowed? role msg)
		      (handle-message-fn this-node, remote-node, msg)
		      (send-error this-node, remote-node, :message-not-allowed, :wrong-node-role, 
		        (format "The node with role \"%s\" is not allowed to send messages of type \"%s\"!" 
		          (pr-str role) (type msg))))))))