package com.health.tracker.domain

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class EnergyMathTest {

    @Test
    fun `每周减 0p5kg 对应每天约 550kcal 缺口`() {
        assertEquals(550.0, EnergyMath.dailyDeficitKcal(0.5), 0.001)
    }

    @Test
    fun `七千七百kcal 缺口换算成一 kg`() {
        assertEquals(1.0, EnergyMath.expectedLossKg(7700.0), 0.001)
    }

    @Test
    fun `缺口为零时不减重`() {
        assertEquals(0.0, EnergyMath.expectedLossKg(0.0), 0.001)
    }

    @Test
    fun `男性基础代谢按 mifflin 计算`() {
        assertEquals(1848.75, EnergyMath.bmrKcal(90.0, 175.0, 30, Sex.MALE), 0.001)
    }

    @Test
    fun `女性基础代谢比同条件男性低 166kcal`() {
        val male = EnergyMath.bmrKcal(90.0, 175.0, 30, Sex.MALE)
        val female = EnergyMath.bmrKcal(90.0, 175.0, 30, Sex.FEMALE)
        assertEquals(166.0, male - female, 0.001)
    }

    @Test
    fun `久坐活动系数为 1p2`() {
        assertEquals(1480.0, EnergyMath.tdeeKcal(1233.3334, ActivityLevel.SEDENTARY), 0.01)
    }

    @Test
    fun `BMI 按米为单位的身高计算`() {
        assertEquals(29.73, EnergyMath.bmi(90.0, 174.0), 0.01)
    }

    @Test
    fun `身高非法时 BMI 归零而不是除零`() {
        assertEquals(0.0, EnergyMath.bmi(90.0, 0.0), 0.0)
    }
}
