package com.masolodilov.jericho.model

enum class InventoryCategory(val title: String) {
    BLUE_POTION("Синее зелье"),
    GREEN_POTION("Зеленое зелье"),
    PURPLE_POTION("Фиолетовое зелье"),
    ANTIBIOTIC("Антибиотик"),
    CHIP_PISTOL("Чип: пистолет (желтый)"),
    CHIP_SHOTGUN("Чип: дробовик (оранжевый)"),
    CHIP_ASSAULT_RIFLE("Чип: штурмовая винтовка (зеленый)"),
    CHIP_SNIPER_RIFLE("Чип: снайперская винтовка (синий)"),
    CHIP_MACHINE_GUN("Чип: пулемет (лиловый)"),
    CHIP_GRENADE("Чип: гранатомет или миномет (фиолетовый)"),
    CHIP_ARMOR_SHIELD("Чип: броня или щит (белый)"),
    CHIP("Чип"),
    ;

    companion object {
        fun selectableEntries(): List<InventoryCategory> {
            return entries.filter { it != CHIP }
        }

        fun fromName(value: String): InventoryCategory {
            return when (value) {
                "POTION" -> BLUE_POTION
                else -> entries.firstOrNull { it.name == value } ?: BLUE_POTION
            }
        }

        fun fromTitle(value: String): InventoryCategory {
            return when (value) {
                "Зелья", "Зелье" -> BLUE_POTION
                "Антибиотики" -> ANTIBIOTIC
                "Чипы" -> CHIP
                else -> entries.firstOrNull { it.title == value } ?: BLUE_POTION
            }
        }
    }
}
