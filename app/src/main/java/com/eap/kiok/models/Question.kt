package com.eap.kiok.models

import com.google.firebase.Timestamp

class Question {
    var question: String = ""
    var answer: String = ""
    var date: Timestamp? = null
    var search_keywords: MutableList<String>? = null

    constructor() {}

    constructor(
        question: String,
        answer: String,
        date: Timestamp,
        search_keywords: MutableList<String>
    ) {
        this.question = question
        this.answer = answer
        this.date = date
        this.search_keywords = search_keywords
    }
}