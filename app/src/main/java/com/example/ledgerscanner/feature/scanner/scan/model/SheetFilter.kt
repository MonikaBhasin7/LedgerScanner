package com.example.ledgerscanner.feature.scanner.scan.model

// Add to a new file or in your model package
enum class SheetFilter {
    ALL,
    HIGH_SCORE,
    LOW_SCORE
}

enum class SheetSort(val displayName: String) {
    DATE_NEWEST("Date (Newest)"),
    DATE_OLDEST("Date (Oldest)"),
    SCORE_HIGH("Score (High to Low)"),
    SCORE_LOW("Score (Low to High)"),
    STUDENT_NAME("Student Name")
}