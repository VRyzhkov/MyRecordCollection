package com.example.myrecordcollection.ui.theme

enum class ThemeMode(val title: String) {
    System("Системная"),
    Light("Светлая"),
    Dark("Тёмная"),
    ;

    companion object {
        fun fromStoredValue(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: System
    }
}
