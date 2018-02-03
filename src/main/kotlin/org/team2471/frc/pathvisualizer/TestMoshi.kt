package org.team2471.frc.pathvisualizer

import com.squareup.moshi.JsonAdapter
//import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import java.util.*

data class BlackjackHand( var hidden_card: Card, var visibleCards: List<Card>)
data class Card(var rank: Char, var suit: Suit)

enum class Suit {
    CLUBS, DIAMONDS, HEARTS, SPADES;
}

class TestMoshi() {

    val blackjackHand = BlackjackHand(
            Card('6', Suit.SPADES),
            Arrays.asList(Card('4', Suit.CLUBS), Card('A', Suit.HEARTS)));

    init {

        val moshi = Moshi.Builder().build()
        val jsonAdapter: JsonAdapter<BlackjackHand> = moshi.adapter(BlackjackHand::class.java)

        // take a class, and use the adapter to convert it to json string
        val json: String = jsonAdapter.toJson(blackjackHand)
        println(json)

        // turn around use the same adapter to turn the string back into kotlin objects
        val blackjackHand2 = jsonAdapter.fromJson(json)
        println(blackjackHand2)
    }
}

/*
        val moshi = Moshi.Builder()
                // Add any other JsonAdapter factories.
                .add(KotlinJsonAdapterFactory())
                .build()
*/
