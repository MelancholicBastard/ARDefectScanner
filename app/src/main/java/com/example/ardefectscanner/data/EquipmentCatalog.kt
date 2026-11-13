package com.example.ardefectscanner.data

data class TechnicalParameter(val name: String, val value: String, val reference: String)
data class EquipmentComponent(val name: String, val description: String)
data class Equipment(
    val id: String,
    val name: String,
    val model: String,
    val location: String,
    val serial: String,
    val installed: String,
    val parameters: List<TechnicalParameter>,
    val components: List<EquipmentComponent>
)

/** Seed data for the prototype. Replace this boundary with a local-server repository later. */
object EquipmentCatalog {
    val equipment = listOf(
        Equipment(
            "PUMP-001", "Насосный агрегат", "К 80-50-200", "Цех № 2 · Контур охлаждения",
            "КСТ-2024-0841", "12.03.2024",
            listOf(
                TechnicalParameter("Подача", "50 м³/ч", "Паспортное значение"),
                TechnicalParameter("Напор", "50 м", "Паспортное значение"),
                TechnicalParameter("Мощность", "15 кВт", "Номинальная"),
                TechnicalParameter("Наработка", "Нет данных", "Нужны данные предприятия")
            ),
            listOf(
                EquipmentComponent("Электродвигатель", "Привод насосного агрегата"),
                EquipmentComponent("Подшипниковый узел", "Контроль нагрева и вибрации"),
                EquipmentComponent("Торцевое уплотнение", "Визуальная проверка герметичности")
            )
        ),
        Equipment(
            "MOTOR-002", "Электродвигатель", "АИР 160 S4", "Цех № 1 · Вентиляционная установка",
            "КСТ-2023-0216", "18.09.2023",
            listOf(
                TechnicalParameter("Мощность", "15 кВт", "Номинальная"),
                TechnicalParameter("Напряжение", "380 В", "Номинальное"),
                TechnicalParameter("Частота вращения", "1500 об/мин", "Синхронная"),
                TechnicalParameter("Наработка", "Нет данных", "Нужны данные предприятия")
            ),
            listOf(
                EquipmentComponent("Корпус", "Визуальная проверка поверхности"),
                EquipmentComponent("Клеммная коробка", "Проверка крышки и кабельных вводов"),
                EquipmentComponent("Вентилятор", "Проверка защитного кожуха")
            )
        )
    )

    fun find(id: String?): Equipment? = equipment.firstOrNull { it.id == id }

    // QR contains an identifier, never an arbitrary URL to fetch.
    fun resolveQr(raw: String): Equipment? {
        val id = raw.trim().removePrefix("KST:")
        return find(id)
    }

    fun qrPayload(equipment: Equipment) = "KST:${equipment.id}"
}
