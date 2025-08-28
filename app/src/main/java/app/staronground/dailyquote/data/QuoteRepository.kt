package app.staronground.dailyquote.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

data class Quote(
    val id: Int,
    val text: String,
    val author: String,
    var favorite: Boolean = false
)

class QuoteRepository {
    private val quotes = mutableListOf(
        Quote(1, "삶이 있는 한 희망은 있다.", "키케로"),
        Quote(2, "나는 생각한다, 고로 존재한다.", "데카르트"),
        Quote(3, "행동은 모든 성공의 기초이다.", "파블로 피카소"),
        Quote(4, "성공은 작은 노력을 반복한 결과다.", "로버트 콜리어"),
        Quote(5, "어제보다 나은 오늘을 만들어라.", "익명")
    )

    private val _current = MutableStateFlow(quotes.first())
    val current = _current.asStateFlow()

    fun next(): Quote {
        val q = quotes[Random.nextInt(quotes.size)]
        _current.value = q
        return q
    }

    fun toggleFavorite(id: Int) {
        quotes.find { it.id == id }?.let { it.favorite = !it.favorite }
    }

    fun favorites(): List<Quote> = quotes.filter { it.favorite }
}
