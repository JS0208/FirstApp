package app.staronground.dailyquote.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Quote(
    val id: Int,
    val text: String,
    val author: String,
    var favorite: Boolean = false
)

class QuoteRepository(private val quotes: MutableList<Quote>) {
    constructor(context: android.content.Context): this(QuoteLoader.loadFromAssets(context).toMutableList())

    private val _current = MutableStateFlow(quotes.first())
    val current = _current.asStateFlow()

    fun next(): Quote {
        val q = quotes.random()
        _current.value = q
        return q
    }

    fun toggleFavorite(id: Int) {
        quotes.find { it.id == id }?.let { it.favorite = !it.favorite }
    }

    fun favorites(): List<Quote> = quotes.filter { it.favorite }
}
