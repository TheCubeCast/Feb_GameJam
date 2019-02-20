package com.dcostap.engine.utils.actions

import com.badlogic.gdx.utils.Array
import com.dcostap.engine.utils.Updatable
import com.thecubecast.ReEngine.Data.DcpUtils.Action

/**
 * Created by Darius on 15/04/2018.
 */
class ActionsUpdater : Updatable {
    private val actions = Array<Action>()
    @Transient private val dummy = Array<Action>()

    val isEmpty
        get() = actions.size == 0

    override fun update(delta: Float) {
        dummy.clear()

        for (action in actions) {
            action.update(delta)

            if (action.hasFinished) {
                dummy.add(action)
            }
        }

        actions.removeAll(dummy, true)
    }

    fun addAction(action: Action) {
        action.reset()
        actions.add(action)
    }

    fun removeAction(action: Action) {
        actions.removeValue(action, true)
    }

    fun clear() {
        actions.clear()
    }
}