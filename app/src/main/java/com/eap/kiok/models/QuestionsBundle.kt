package com.eap.kiok.models

import com.google.firebase.Timestamp

class QuestionsBundle {
    var name: String = ""
    var quantity: Int = 0
    var date: Timestamp? = null
    var search_keywords: MutableList<String>? = null

    constructor() {}

    constructor(name: String, quantity: Int, date: Timestamp, search_keywords: MutableList<String>) {
        this.name = name
        this.quantity = quantity
        this.date = date
        this.search_keywords = search_keywords
    }
}