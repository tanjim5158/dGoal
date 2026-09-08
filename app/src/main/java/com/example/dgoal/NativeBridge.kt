package com.example.dgoal

object NativeBridge {

    init {
        System.loadLibrary("dgoal")
    }

    external fun calculateProgress(
        completed: Int,
        total: Int
    ): Int
}