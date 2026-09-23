package com.example.model

enum class PracticeModeType {
    PRICE_ACTION_PREDICTION,
    BREAKOUT_QUIZ,
    RETEST_QUIZ,
    FALSE_BREAKOUT_QUIZ
}

data class PracticeExercise(
    val id: String,
    val title: String,
    val description: String,
    val modeType: PracticeModeType,
    val candles: List<Candle>,
    val hiddenIndex: Int,
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String
)
