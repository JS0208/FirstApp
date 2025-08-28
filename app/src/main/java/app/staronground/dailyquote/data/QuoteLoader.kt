package app.staronground.dailyquote.data

import android.content.Context
import org.json.JSONArray

object QuoteLoader {
    fun loadFromAssets(context: Context): List<Quote> = try {
        val json = context.assets.open("quotes.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arr = JSONArray(json)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    Quote(
                        id = o.optInt("id", i + 1),
                        text = o.getString("text"),
                        author = o.getString("author"),
                        favorite = false
                    )
                )
            }
        }
    } catch (e: Exception) {
        listOf(
            Quote(1, "삶이 있는 한 희망은 있다.", "키케로"),
            Quote(2, "나는 생각한다, 고로 존재한다.", "데카르트")
        )
    }
}
